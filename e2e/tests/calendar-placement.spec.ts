import { randomUUID } from 'node:crypto';
import { expect, test, type Locator } from '@playwright/test';

test.use({ timezoneId: 'UTC' });

type ShiftColumn = { columnIndex: number; shiftDto: { name: string } };
type Lane = { location?: { id: string }; shiftColumns: ShiftColumn[] };
type Shift = { name: string; start: number; end: number; column: number };

function card(grid: Locator, name: string) {
  return grid.getByText(name, { exact: true }).locator('..');
}

for (const pattern of ['simultaneous', 'staggered'] as const) {
  test(`packs two to four ${pattern} shifts side by side in each calendar lane`, async ({ page }) => {
    await page.goto('/events');
    await page.getByRole('button', { name: 'Log In', exact: true }).click();
    await page.getByLabel(/username/i).fill('dev');
    await page.getByLabel('Password', { exact: true }).fill('dev');
    const [eventsResponse] = await Promise.all([
      page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/events' && response.ok()),
      page.getByRole('button', { name: 'Sign In', exact: true }).click(),
    ]);
    const authorization = await eventsResponse.request().headerValue('authorization');
    expect(authorization).toBeTruthy();
    const headers = { authorization: authorization! };
    const start = new Date();
    start.setUTCHours(0, 0, 0, 0);
    const atMinute = (minute: number) => new Date(start.getTime() + minute * 60_000).toISOString();
    const date = atMinute(0).slice(0, 10);

    const eventResponse = await page.request.post('/api/v1/events', {
      headers,
      data: { name: `Placement test ${randomUUID().slice(0, 8)}`, startTime: atMinute(0), endTime: atMinute(1439) },
    });
    await expect(eventResponse).toBeOK();
    const { id: eventId } = await eventResponse.json();
    const eventPath = `/api/v1/events/${eventId}`;

    try {
      const planResponse = await page.request.post(`${eventPath}/shift-plans`, {
        headers, data: { name: 'Operations', defaultNoRolePointsPerMinute: 0 },
      });
      await expect(planResponse).toBeOK();
      const { shiftPlan, plannerInvite } = await planResponse.json();
      await expect(await page.request.post('/api/v1/join', {
        headers, data: { inviteCode: plannerInvite.code },
      })).toBeOK();

      const locationResponse = await page.request.post(`${eventPath}/locations`, {
        headers, data: { name: 'Main Hall' },
      });
      await expect(locationResponse).toBeOK();
      const { id: locationId } = await locationResponse.json();
      const lanes = [
        { id: locationId, title: 'Main Hall', prefix: 'Hall' },
        { id: undefined, title: 'Without Location', prefix: 'Unlocated' },
      ].map(lane => ({
        ...lane,
        groups: [2, 3, 4].map((count, groupIndex): Shift[] => {
          const groupStart = 180 + groupIndex * 300;
          const shifts = Array.from({ length: count }, (_, index) => {
            const start = groupStart + (pattern === 'staggered' ? index * 10 : 0);
            return { name: `${lane.prefix} ${count}-${index + 1}`, start, end: start + 90, column: index };
          });
          return [...shifts, {
            name: `${lane.prefix} ${count} reuse`, start: groupStart + 90, end: groupStart + 105, column: 0,
          }];
        }),
      }));

      for (const lane of lanes) {
        // Insert later starts first to exercise backend ordering independently of insertion order.
        for (const shift of lane.groups.flat().reverse()) {
          await expect(await page.request.post(`/api/v1/shift-plans/${shiftPlan.id}/shifts`, {
            headers,
            data: {
              name: shift.name, startTime: atMinute(shift.start), endTime: atMinute(shift.end),
              locationId: lane.id, bonusRewardPoints: 0,
            },
          })).toBeOK();
        }
      }

      const layoutResponse = await page.request.get(`${eventPath}/schedule/layout`, { headers });
      await expect(layoutResponse).toBeOK();
      const layout = await layoutResponse.json();
      expect(layout.scheduleLayoutDtos.find((lane: Lane) => lane.location?.id === locationId).requiredShiftColumns).toBe(4);
      expect(layout.scheduleLayoutNoLocationDto.requiredShiftColumns).toBe(4);

      const contentResponse = await page.request.get(`${eventPath}/schedule/content?date=${date}`, { headers });
      await expect(contentResponse).toBeOK();
      const content = await contentResponse.json();
      await page.goto(`/events/${eventId}/calendar`);
      const grid = page.locator('app-shift-calendar-grid');

      for (const lane of lanes) {
        const { shiftColumns }: Lane = lane.id === undefined
          ? content.scheduleContentNoLocationDto
          : content.scheduleContentDtos.find((entry: Lane) => entry.location?.id === lane.id);
        expect(shiftColumns.map(column => column.shiftDto.name).sort()).toEqual(
          lane.groups.flat().map(shift => shift.name).sort(),
        );
        const columnsByName = new Map(shiftColumns.map(entry => [entry.shiftDto.name, entry.columnIndex]));
        const heading = grid.getByText(lane.title, { exact: true }).locator('..');
        await expect(heading).toBeVisible();

        for (const group of lane.groups) {
          const overlapping = group.slice(0, -1);
          expect(overlapping.map(shift => columnsByName.get(shift.name)).sort()).toEqual(
            overlapping.map(shift => shift.column),
          );
          if (pattern === 'staggered') {
            expect(overlapping.map(shift => columnsByName.get(shift.name))).toEqual(overlapping.map(shift => shift.column));
          }
          expect(columnsByName.get(group.at(-1)!.name)).toBe(0);

          for (const shift of group) {
            const item = card(grid, shift.name);
            await expect(item).toBeVisible();
            const column = columnsByName.get(shift.name)!;
            await expect(item).toHaveCSS('grid-column-start', `venue-${lane.id ?? 'none'}-shift-col-${column + 1}-start`);
          }
          // Sample the cards in the same frame while the grid may still be scrolling.
          const cardBounds = new Map(await grid.locator('.item-primary').evaluateAll(items => items.map(item => {
            const { x, y, width, height } = item.getBoundingClientRect();
            return [item.querySelector('.label')?.textContent, { x, y, width, height }] as const;
          })));
          const boxes = group.map(shift => ({
            shift, column: columnsByName.get(shift.name)!, box: cardBounds.get(shift.name)!,
          }));
          const reference = boxes[0];
          const pixelsPerMinute = reference.box.height / (reference.shift.end - reference.shift.start);
          for (const { shift, box } of boxes) {
            expect(box.y - reference.box.y).toBeCloseTo((shift.start - reference.shift.start) * pixelsPerMinute, 0);
            expect(box.height).toBeCloseTo((shift.end - shift.start) * pixelsPerMinute, 0);
          }

          const header = (await heading.boundingBox())!;
          const sorted = boxes.slice(0, -1).sort((a, b) => a.column - b.column);
          for (const [index, { box }] of sorted.entries()) {
            expect(box.x).toBeGreaterThanOrEqual(header.x - 1);
            expect(box.x + box.width).toBeLessThanOrEqual(header.x + header.width + 1);
            expect(box.width).toBeGreaterThan(0);
            expect(box.width).toBeCloseTo(sorted[0].box.width, 0);
            if (index > 0) {
              const previous = sorted[index - 1].box;
              expect(box.x).toBeGreaterThanOrEqual(previous.x + previous.width - 1);
            }
          }
          const reused = boxes.at(-1)!.box;
          expect(reused.x).toBeCloseTo(sorted[0].box.x, 0);
          expect(reused.width).toBeCloseTo(sorted[0].box.width, 0);
        }
      }
    } finally {
      await expect(await page.request.delete(eventPath, { headers })).toBeOK();
    }
  });
}
