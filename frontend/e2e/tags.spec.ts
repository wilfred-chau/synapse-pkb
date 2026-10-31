import { expect, test } from '@playwright/test';

test('中文标签管理、刷新持久化、错误与登录失效', async ({ page }) => {
  const username = process.env.A2_E2E_USERNAME;
  const password = process.env.A2_E2E_PASSWORD;
  if (!username || !password) throw new Error('A2 E2E credentials must come from local configuration');
  const logs: Promise<string>[] = [];
  const pageErrors: string[] = [];
  page.on('console', message => logs.push(
    Promise.all(message.args().map(arg => arg.jsonValue()))
      .then(values => JSON.stringify(values)).catch(() => message.text())
  ));
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.goto('/');
  await page.getByLabel('用户名', { exact: true }).fill(username);
  await page.getByLabel('密码', { exact: true }).fill(password);
  const loginResponse = page.waitForResponse(response => response.url().endsWith('/api/auth/login') && response.status() === 200);
  await page.getByRole('button', { name: /登\s*录/ }).click();
  const token = (await (await loginResponse).json()).data.accessToken as string;
  await expect(page.getByRole('tab', { name: '分类与标签' })).toBeVisible();
  await expect(page.getByText('技术资料', { exact: true })).toBeVisible();
  await expect(page.getByText('生活笔记', { exact: true })).toBeVisible();
  await expect(page.getByText('当前为手动标签管理')).toBeVisible();

  const prefix = `测试${Date.now()}`;
  const source = `${prefix}机器学习`;
  const renamed = `${prefix}深度学习`;
  const target = `${prefix}技术笔记`;
  const dialog = page.getByRole('dialog');
  const row = (name: string) => page.getByRole('listitem').filter({ has: page.getByText(name, { exact: true }) });
  const create = async (name: string) => {
    await page.getByRole('button', { name: '创建标签', exact: true }).click();
    await dialog.getByLabel('标签名称').fill(name);
    await dialog.getByRole('button', { name: /保\s*存/ }).click();
    await expect(dialog).not.toBeVisible();
    await expect(row(name.trim())).toBeVisible();
  };

  await create(` ${source} `);
  await page.getByRole('button', { name: '创建标签', exact: true }).click();
  await dialog.getByLabel('标签名称').fill(source);
  await dialog.getByRole('button', { name: /保\s*存/ }).click();
  await expect(dialog.getByText(/已有同名标签/)).toBeVisible();
  await dialog.getByRole('button', { name: /取\s*消/ }).click();

  await row(source).getByRole('button', { name: '重命名' }).click();
  await dialog.getByLabel('标签名称').fill(renamed);
  await dialog.getByRole('button', { name: /保\s*存/ }).click();
  await expect(dialog).not.toBeVisible();
  await expect(row(renamed)).toBeVisible();
  await create(target);
  await row(renamed).getByRole('button', { name: /合\s*并/ }).click();
  await dialog.getByRole('combobox').click();
  await page.getByTitle(target, { exact: true }).click();
  await dialog.getByRole('button', { name: '确认合并' }).click();
  await expect(dialog).not.toBeVisible();
  await expect(row(renamed)).toHaveCount(0);
  await expect(row(target)).toBeVisible();
  await page.reload();
  await expect(row(target)).toBeVisible();
  await expect(row(source)).toHaveCount(0);
  await expect(row(renamed)).toHaveCount(0);

  await page.getByLabel('搜索标签').fill('不存在的标签');
  await expect(page.getByText('没有匹配的标签')).toBeVisible();
  await page.getByLabel('搜索标签').fill('');
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('button', { name: '创建标签', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: '../backend/target/a2-mobile.png', fullPage: true });
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.screenshot({ path: '../backend/target/a2-desktop.png', fullPage: true });

  const logLines = await Promise.all(logs);
  expect(logLines.some(line => line.includes(password) || line.includes(token))).toBe(false);
  expect(logLines.some(line => line.includes('accessToken') || line.includes('password:'))).toBe(false);
  expect(pageErrors).toEqual([]);
  // Only this error-state case is simulated. All management writes above use the real server.
  await page.route('**/api/tags', route => route.fulfill({
    status: 401, contentType: 'application/json',
    body: JSON.stringify({ success: false, data: null, error: { code: 'AUTHENTICATION_REQUIRED', message: 'Expired', details: {} } })
  }));
  await page.getByRole('button', { name: /刷\s*新/ }).click();
  await expect(page.getByRole('heading', { name: '登录 Synapse PKB' })).toBeVisible();
});
