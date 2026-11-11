import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

test('an admin creates an event, shift plan, and volunteer role', async ({ page }) => {
  const eventName = `Playwright Event ${randomUUID().slice(0, 8)}`;
  const planName = 'Operations';
  const roleName = 'Check-in Volunteer';

  await test.step('Sign in through Keycloak as the local platform admin', async () => {
    await page.goto('/events');
    await page.getByRole('button', { name: 'Log In', exact: true }).click();
    await page.getByLabel(/username/i).fill('dev');
    await page.getByLabel('Password', { exact: true }).fill('dev');
    await page.getByRole('button', { name: 'Sign In', exact: true }).click();
    await expect(page).toHaveURL(/\/events$/);
  });

  await test.step('Create an event', async () => {
    await page.getByRole('heading', { name: 'Create new event' }).click();
    await page.getByPlaceholder('Event name', { exact: true }).fill(eventName);
    await page.getByRole('button', { name: 'Create Event', exact: true }).click();
    await expect(page).toHaveURL(/\/events\/[^/]+\/manage$/);
    await expect(page.getByPlaceholder('Event name', { exact: true })).toHaveValue(eventName);
  });

  await test.step('Create a shift plan and open its Roles page', async () => {
    await page.getByRole('button', { name: 'Shift Plans', exact: true }).click();
    const newPlan = page.locator('app-manage-plan-details').filter({
      has: page.getByText('Create Shift Plan', { exact: true }),
    });
    await newPlan.getByPlaceholder('Plan name', { exact: true }).fill(planName);
    await newPlan.getByRole('button', { name: 'Create', exact: true }).click();

    const savedPlan = page.locator('app-manage-plan-details').filter({
      has: page.getByRole('button', { name: 'Save', exact: true }),
    });
    await expect(savedPlan.getByPlaceholder('Plan name', { exact: true })).toHaveValue(planName);

    await page.getByRole('link', { name: 'plan management' }).click();
    await page.locator('#event-plans-plan').getByRole('button').click();
    await page.getByRole('option', { name: planName, exact: true }).click();
    await page.getByRole('button', { name: 'Roles', exact: true }).click();
    await expect(page.getByRole('heading', { name: `${planName}: Roles` })).toBeVisible();
  });

  await test.step('Create a volunteer role and verify it persists after reload', async () => {
    const newRole = page.locator('app-manage-role').filter({
      has: page.getByText('Create Volunteer Role', { exact: true }),
    });
    await newRole.getByPlaceholder('Role name', { exact: true }).fill(roleName);
    await newRole.getByRole('button', { name: 'Create', exact: true }).click();

    const savedRole = page.locator('app-manage-role').filter({
      has: page.getByRole('button', { name: 'Save', exact: true }),
    });
    const savedRoleName = savedRole.getByPlaceholder('Role name', { exact: true });
    await expect(savedRoleName).toHaveValue(roleName);
    await page.reload();
    await expect(page.getByRole('heading', { name: `${planName}: Roles` })).toBeVisible();
    await expect(savedRoleName).toHaveValue(roleName);
  });

  console.log(`Created "${eventName}" → "${planName}" → "${roleName}" at ${page.url()}`);
});
