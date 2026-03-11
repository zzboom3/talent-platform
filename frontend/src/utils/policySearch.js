function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function splitPolicyKeywords(text) {
  return String(text || '')
    .split(/[,，/|；;、\n\s]+/)
    .map(item => item.trim())
    .filter(item => item.length >= 2 && item.length <= 20)
    .slice(0, 12)
}

function getPolicySearchText(item) {
  return [
    item.title,
    item.content,
    item.author,
    item.sourceSite,
    item.category,
  ]
    .map(normalizeText)
    .filter(Boolean)
    .join(' ')
}

export function filterPolicies(items, filters = {}) {
  const keyword = normalizeText(filters.keyword)
  const sourceSite = normalizeText(filters.sourceSite)

  return items.filter((item) => {
    const searchText = getPolicySearchText(item)
    const matchKeyword = !keyword || searchText.includes(keyword)
    const matchSourceSite = !sourceSite || normalizeText(item.sourceSite) === sourceSite

    return matchKeyword && matchSourceSite
  })
}

export function buildPolicyFilterOptions(items) {
  const sourceSites = new Set()
  const keywordCountMap = new Map()

  items.forEach((item) => {
    const sourceSite = String(item.sourceSite || '').trim()
    if (sourceSite) sourceSites.add(sourceSite)

    ;[item.title, item.content, item.sourceSite].forEach((text) => {
      splitPolicyKeywords(text).forEach((term) => {
        keywordCountMap.set(term, (keywordCountMap.get(term) || 0) + 1)
      })
    })
  })

  const hotKeywords = [...keywordCountMap.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'zh-CN'))
    .slice(0, 8)
    .map(([term]) => term)

  return {
    sourceSites: [...sourceSites].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    keywords: hotKeywords,
  }
}
