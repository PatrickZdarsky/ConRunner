import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

test.use({ timezoneId: 'UTC' });

test('calendar activities refresh across views and their dialogs close with Escape', async ({ page }) => {
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
  const end = new Date(start);
  end.setUTCHours(23, 59, 59, 999);
  const suffix = randomUUID().slice(0, 8);
  const eventResponse = await page.request.post('/api/v1/events', {
    headers,
    data: { name: `Calendar test ${suffix}`, startTime: start.toISOString(), endTime: end.toISOString() },
  });
  await expect(eventResponse).toBeOK();
  const { id: eventId } = await eventResponse.json();

  try {
    const planResponse = await page.request.post(`/api/v1/events/${eventId}/shift-plans`, {
      headers,
      data: { name: 'Operations', defaultNoRolePointsPerMinute: 0 },
    });
    await expect(planResponse).toBeOK();

    await page.goto(`/events/${eventId}/calendar`);
    await page.getByRole('button', { name: 'Schedule', exact: true }).click();

    const grid = page.locator('app-shift-calendar-grid');
    const noLocationColumn = grid.locator('.venue-flow').last();
    await expect(noLocationColumn).toBeVisible();
    const column = await noLocationColumn.boundingBox();
    // Click between hour markers so the calendar receives the click.
    const entryPosition = { x: 20, y: column!.height * 0.52 };
    await noLocationColumn.click({ position: entryPosition, timeout: 5_000 });

    const activityNameInput = page.getByPlaceholder('Activity name', { exact: true });
    await activityNameInput.fill('Unsaved draft');
    await activityNameInput.press('Escape');
    await expect(activityNameInput).toBeHidden();
    await expect(grid.getByText('Unsaved draft', { exact: true })).toHaveCount(0);

    await noLocationColumn.click({ position: entryPosition, timeout: 5_000 });
    const activityName = `Opening ${suffix}`;
    await activityNameInput.fill(activityName);
    await page.getByRole('button', { name: 'Create Activity', exact: true }).click();

    const activity = grid.getByText(activityName, { exact: true });
    await expect(grid.locator('.item-primary').getByText(activityName, { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Shifts', exact: true }).click();
    await expect(grid.locator('.item-secondary').getByText(activityName, { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Schedule', exact: true }).click();
    await expect(grid.locator('.item-primary').getByText(activityName, { exact: true })).toBeVisible();
    await expect(activity).toHaveCount(1);

    await activity.click();
    await page.getByRole('button', { name: 'Delete Activity', exact: true }).click();
    const confirmation = page.locator('.cdk-overlay-pane').last();
    await expect(confirmation.getByRole('heading', { name: 'Delete Activity' })).toBeVisible();
    await confirmation.getByRole('button', { name: 'Delete Activity', exact: true }).press('Escape');
    await expect(page.getByRole('heading', { name: 'Delete Activity' })).toBeHidden();
    await expect(activityNameInput).toHaveValue(activityName);

    await activityNameInput.fill('Unsaved edit');
    await page.getByRole('button', { name: 'Update Activity', exact: true }).press('Escape');
    await expect(activityNameInput).toBeHidden();
    await expect(activity).toBeVisible();
    await expect(grid.getByText('Unsaved edit', { exact: true })).toHaveCount(0);
  } finally {
    await expect(await page.request.delete(`/api/v1/events/${eventId}`, { headers })).toBeOK();
  }
});
