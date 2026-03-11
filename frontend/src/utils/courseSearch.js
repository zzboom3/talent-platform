function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function splitCourseKeywords(text) {
  return String(text || '')
    .split(/[,，/|；;、\n\s]+/)
    .map(item => item.trim())
    .filter(item => item.length >= 2 && item.length <= 20)
    .slice(0, 12)
}

function getCourseSearchText(course) {
  return [
    course.title,
    course.description,
    course.teacher,
    course.category,
  ]
    .map(normalizeText)
    .filter(Boolean)
    .join(' ')
}

export function filterCourses(courses, filters = {}) {
  const keyword = normalizeText(filters.keyword)
  const category = normalizeText(filters.category)
  const teacher = normalizeText(filters.teacher)

  return courses.filter((course) => {
    const searchText = getCourseSearchText(course)
    const matchKeyword = !keyword || searchText.includes(keyword)
    const matchCategory = !category || normalizeText(course.category) === category
    const matchTeacher = !teacher || normalizeText(course.teacher) === teacher

    return matchKeyword && matchCategory && matchTeacher
  })
}

export function buildCourseFilterOptions(courses) {
  const categories = new Set()
  const teachers = new Set()
  const keywordCountMap = new Map()

  courses.forEach((course) => {
    const category = String(course.category || '').trim()
    const teacher = String(course.teacher || '').trim()

    if (category) categories.add(category)
    if (teacher) teachers.add(teacher)

    ;[course.title, course.description, course.category].forEach((text) => {
      splitCourseKeywords(text).forEach((term) => {
        keywordCountMap.set(term, (keywordCountMap.get(term) || 0) + 1)
      })
    })
  })

  const hotKeywords = [...keywordCountMap.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'zh-CN'))
    .slice(0, 8)
    .map(([term]) => term)

  return {
    categories: [...categories].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    teachers: [...teachers].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    keywords: hotKeywords,
  }
}
