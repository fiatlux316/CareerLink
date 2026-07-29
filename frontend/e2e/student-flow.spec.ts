import { expect, test, type Page } from '@playwright/test'

type ConsultationTypeResponse = {
  id: number
  name: string
  description: string
}

type ConsultationResponse = {
  id: string
  typeName: string
  status: string
}

const createStudentIdentity = () => {
  const seed = Date.now().toString().slice(-8)

  return {
    name: `학생${seed.slice(-4)}`,
    phone: `010${seed}`,
  }
}

const enterStudent = async (page: Page, name: string, phone: string) => {
  await page.goto('/receive')
  await page.getByLabel('\uc774\ub984').fill(name)
  await page.getByLabel('\ud734\ub300\ud3f0 \ubc88\ud638').fill(phone)
  await page.getByLabel('\uc911\ud559\uad50').check()
  await page.getByLabel('1\ud559\ub144').check()

  const [typesResponse] = await Promise.all([
    page.waitForResponse((response) => {
      return response.url().includes('/api/types') && response.request().method() === 'GET' && response.status() === 200
    }),
    page.getByRole('button', { name: '\uc785\uc7a5\ud558\uae30' }).click(),
  ])

  await expect(page).toHaveURL(/\/home$/)

  return (await typesResponse.json()) as ConsultationTypeResponse[]
}

const submitConsultation = async (page: Page, typeIndex: number) => {
  const typeRadio = page.locator('input[name="typeId"]').nth(typeIndex)
  await expect(typeRadio).toBeVisible()
  await typeRadio.check()

  const [consultationResponse] = await Promise.all([
    page.waitForResponse((response) => {
      return response.url().includes('/api/consultations') && response.request().method() === 'POST' && response.status() === 201
    }),
    page.getByRole('button', { name: '상담신청' }).click(),
  ])

  return (await consultationResponse.json()) as ConsultationResponse
}

test.describe('학생 PRD v3 플로우', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/receive')
    await page.evaluate(() => {
      window.localStorage.clear()
    })
  })

  test('학생이 /receive에서 입장 후 /home에서 상담을 신청할 수 있다', async ({ page }) => {
    const student = createStudentIdentity()
    const types = await enterStudent(page, student.name, student.phone)

    expect(types.length).toBeGreaterThan(0)
    await expect(page.getByRole('heading', { name: `${student.name}님, 환영합니다` })).toBeVisible()

    const createdConsultation = await submitConsultation(page, 0)

    await expect(page.locator('.completion-alert')).toContainText(`신청 완료: ${createdConsultation.typeName}`)
    await expect(page.getByRole('button', { name: '전체 목록 보기' })).toBeVisible()
  })

  test('학생이 연속으로 2건 신청하면 /status 목록에 모두 표시된다', async ({ page }) => {
    const student = createStudentIdentity()
    const types = await enterStudent(page, student.name, student.phone)

    expect(types.length).toBeGreaterThan(1)

    const firstConsultation = await submitConsultation(page, 0)
    await expect(page.locator('.completion-alert')).toContainText(firstConsultation.typeName)

    const secondConsultation = await submitConsultation(page, 1)
    await expect(page.locator('.completion-alert')).toContainText(secondConsultation.typeName)

    await page.getByRole('button', { name: '내 상담 목록 보기' }).click()

    await expect(page).toHaveURL(/\/status$/)
    await expect(page.locator('.consultation-item')).toHaveCount(2)
    await expect(page.locator('.consultation-item').first()).toContainText(secondConsultation.typeName)
    await expect(page.locator('.consultation-item').nth(1)).toContainText(firstConsultation.typeName)
  })

  test('RECEIVED 상태 상담은 상세 화면에서 취소할 수 있다', async ({ page }) => {
    const student = createStudentIdentity()
    const types = await enterStudent(page, student.name, student.phone)

    expect(types.length).toBeGreaterThan(0)

    await submitConsultation(page, 0)

    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('정말로 상담을 취소하시겠습니까?')
      await dialog.accept()
    })

    await page.getByRole('button', { name: '상세보기' }).click()
    await expect(page).toHaveURL(/\/status\/\d+$/)
    await expect(page.locator('.status-view__top-bar .badge-received')).toHaveText('접수완료')

    const [cancelResponse] = await Promise.all([
      page.waitForResponse((response) => {
        return response.url().includes('/cancel') && response.request().method() === 'PATCH'
      }),
      page.getByRole('button', { name: '상담 취소' }).click(),
    ])

    expect(cancelResponse.status(), await cancelResponse.text()).toBe(200)

    await expect(page.locator('.status-view__top-bar .badge-cancelled')).toHaveText('취소됨')
    await expect(page.locator('.cancelled-message')).toContainText('상담이 취소되었습니다')
  })
})
