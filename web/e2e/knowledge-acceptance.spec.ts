import { expect, test, type Page } from "@playwright/test"

const EMAIL = process.env.PLAYWRIGHT_EMAIL ?? "liusx1024@gmail.com"
const PASSWORD = process.env.PLAYWRIGHT_PASSWORD ?? "#L194140"
const DATASET_NAME = "Visual Acceptance Dataset"

async function login(page: Page) {
  await page.goto("/sign-in")
  await expect(page.getByLabel("邮箱")).toHaveValue(EMAIL)
  await expect(page.getByRole("textbox", { name: "密码" })).toHaveValue(
    PASSWORD
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/auth/login") &&
      response.request().method() === "POST"
  )
  await page.getByRole("button", { name: "登录", exact: true }).click()
  const response = await responsePromise
  const payload = (await response.json()) as {
    data: { tokenValue: string }
  }
  await expect(page).toHaveURL(/\/$/)
  return payload.data.tokenValue
}

async function startNewConversation(page: Page, mobile: boolean) {
  if (mobile) {
    await page.getByRole("button", { name: "Toggle Sidebar" }).first().click()
  }
  await page.getByLabel("新建对话").click()
  if (mobile) {
    await page.keyboard.press("Escape")
    await expect(page.getByRole("dialog", { name: "Sidebar" })).toBeHidden()
  }
}

async function sendFirstMessage(page: Page, message: string) {
  const sessionPromise = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/conversations") &&
      response.request().method() === "POST"
  )
  const composer = page.getByPlaceholder(
    "询问知识库、粘贴材料，或描述要分析的问题..."
  )
  await composer.fill(message)
  await page.getByLabel("Send message").click()
  const response = await sessionPromise
  const payload = (await response.json()) as { data: { id: string } }
  return payload.data.id
}

async function expectNoHorizontalOverflow(page: Page) {
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1
      )
    )
    .toBe(true)
}

test("knowledge answers and refusal evaluation remain usable", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000)
  const sessionIds: string[] = []
  const accessToken = await login(page)
  const mobile = testInfo.project.name === "mobile"
  try {
    await expect(page.getByLabel("企业知识问答助手")).toBeVisible()
    await startNewConversation(page, mobile)

    sessionIds.push(
      await sendFirstMessage(page, "请介绍一下你自己，你能做什么？")
    )
    await expect(
      page.getByText(/我是\s*KnowStudio\s*的中文智能助手/).last()
    ).toBeVisible()
    await expectNoHorizontalOverflow(page)

    await startNewConversation(page, mobile)

    sessionIds.push(
      await sendFirstMessage(
        page,
        "费用报销 客户拜访产生的交通、餐饮和住宿费用分别怎么报"
      )
    )
    await expect(
      page.getByText(
        "当前知识库中没有找到与该问题相关的可靠资料，无法依据现有文档回答。"
      )
    ).toBeVisible()

    const composer = page.getByPlaceholder(
      "询问知识库、粘贴材料，或描述要分析的问题..."
    )
    await composer.fill("Java 的索引如何命名？")
    await page.getByLabel("Send message").click()
    await expect(page.getByLabel("Stop generation")).toBeHidden({
      timeout: 45_000,
    })
    await expect(page.getByText(/pk_/).last()).toBeVisible()
    await expect(page.getByText(/uk_/).last()).toBeVisible()
    await expect(page.getByText(/idx_/).last()).toBeVisible()
    await expect(
      page.locator("li").filter({ hasText: /主键索引/ }).last()
    ).toBeVisible()
    await expect(
      page.locator("li").filter({ hasText: /唯一索引/ }).last()
    ).toBeVisible()
    await expect(
      page.locator("li").filter({ hasText: /普通索引/ }).last()
    ).toBeVisible()
    await expect(page.getByText(/varchar 字段上建立索引/)).toHaveCount(0)
    await expectNoHorizontalOverflow(page)
    await page.screenshot({
      path: testInfo.outputPath("knowledge-natural-answer.png"),
      fullPage: true,
    })

    await composer.fill("那常量呢？")
    await page.getByLabel("Send message").click()
    await expect(page.getByLabel("Stop generation")).toBeHidden({
      timeout: 45_000,
    })
    await expect(page.getByText(/MAX_STOCK_COUNT/).last()).toBeVisible()
    await expect(page.getByText(/CACHE_EXPIRED_TIME/).last()).toBeVisible()
    await expect(page.getByText("请补充更具体的问题或查询对象。")).toHaveCount(0)
    await expectNoHorizontalOverflow(page)
    await page.screenshot({
      path: testInfo.outputPath("knowledge-follow-up-answer.png"),
      fullPage: true,
    })

    await page.goto("/admin/evaluations")
    await expect(
      page.getByRole("heading", { name: "检索消融评测" })
    ).toBeVisible()
    const existingDataset = page.getByText(DATASET_NAME, { exact: true })
    if ((await existingDataset.count()) === 0) {
      await page.getByRole("button", { name: "新建数据集" }).first().click()
      await page.getByLabel("名称").fill(DATASET_NAME)
      await page.getByRole("button", { name: "创建", exact: true }).click()
    }
    await page.getByRole("button", { name: "添加样本" }).click()
    await page.getByText("预期系统拒答").click()
    await expect(page.getByLabel("相关 Chunk ID")).toBeDisabled()
    await expectNoHorizontalOverflow(page)

    await page.screenshot({
      path: testInfo.outputPath("evaluation-refusal-form.png"),
      fullPage: true,
    })
  } finally {
    for (const sessionId of sessionIds) {
      await page.request.delete(`/api/conversations/${sessionId}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
    }
  }
})
