function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function parseSalaryValue(value, unit) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return null

  if (unit === 'w' || unit === 'W' || unit === '万') return amount * 10
  if (unit === '千') return amount
  return amount
}

export function parseJobSalaryRange(text) {
  const matches = [...String(text || '').matchAll(/(\d+(?:\.\d+)?)\s*([kKwW万千]?)/g)]
    .map(([, value, unit]) => parseSalaryValue(value, unit))
    .filter(value => value != null)

  if (!matches.length) return null

  const sorted = matches.sort((a, b) => a - b)
  return {
    min: sorted[0],
    max: sorted[sorted.length - 1],
  }
}

export function formatSalaryK(value) {
  if (!Number.isFinite(value)) return ''
  if (value >= 10 && value % 10 === 0) return `${value / 10}万`
  return `${value}k`
}

function getJobSearchText(job) {
  return [
    job.title,
    job.description,
    job.requirements,
    job.salaryRange,
    job.city,
    job.company?.companyName,
  ]
    .map(normalizeText)
    .filter(Boolean)
    .join(' ')
}

export function filterJobs(jobs, filters = {}) {
  const keyword = normalizeText(filters.keyword)
  const city = normalizeText(filters.city)
  const companyName = normalizeText(filters.companyName)
  const salaryRange = Array.isArray(filters.salaryRange) && filters.salaryRange.length === 2
    ? filters.salaryRange
    : null

  return jobs.filter((job) => {
    const searchText = getJobSearchText(job)
    const jobSalaryRange = parseJobSalaryRange(job.salaryRange)
    const matchKeyword = !keyword || searchText.includes(keyword)
    const matchCity = !city || normalizeText(job.city) === city
    const matchCompany = !companyName || normalizeText(job.company?.companyName) === companyName
    const matchSalary = !salaryRange
      || (jobSalaryRange && jobSalaryRange.max >= salaryRange[0] && jobSalaryRange.min <= salaryRange[1])

    return matchKeyword && matchCity && matchSalary && matchCompany
  })
}

export function buildJobFilterOptions(jobs) {
  const cities = new Set()
  const companies = new Set()
  const salaryValues = []

  jobs.forEach((job) => {
    const city = String(job.city || '').trim()
    const companyName = String(job.company?.companyName || '').trim()
    const salaryRange = parseJobSalaryRange(job.salaryRange)

    if (city) cities.add(city)
    if (companyName) companies.add(companyName)
    if (salaryRange) {
      salaryValues.push(salaryRange.min, salaryRange.max)
    }
  })

  const salaryBounds = salaryValues.length
    ? {
        min: Math.floor(Math.min(...salaryValues)),
        max: Math.ceil(Math.max(...salaryValues)),
      }
    : null

  return {
    cities: [...cities].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    companies: [...companies].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    salaryBounds,
  }
}
