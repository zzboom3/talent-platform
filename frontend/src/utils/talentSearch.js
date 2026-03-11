function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

export function splitTalentSkills(skills) {
  return String(skills || '')
    .split(/[,，/|\s]+/)
    .map(item => item.trim())
    .filter(Boolean)
    .slice(0, 8)
}

function getTalentSearchText(talent) {
  return [
    talent.realName,
    talent.user?.username,
    talent.education,
    talent.gender,
    talent.graduationSchool,
    talent.major,
    talent.workYears,
    talent.expectedPosition,
    talent.expectedSalary,
    talent.experience,
    talent.projectExperience,
    talent.selfIntroduction,
    talent.certificates,
    talent.city,
    talent.skills,
  ]
    .map(normalizeText)
    .filter(Boolean)
    .join(' ')
}

export function filterTalents(talents, filters = {}) {
  const keyword = normalizeText(filters.keyword)
  const city = normalizeText(filters.city)
  const education = normalizeText(filters.education)
  const skill = normalizeText(filters.skill)

  return talents.filter((talent) => {
    const searchText = getTalentSearchText(talent)
    const skills = splitTalentSkills(talent.skills).map(normalizeText)

    const matchKeyword = !keyword || searchText.includes(keyword)
    const matchCity = !city || normalizeText(talent.city) === city
    const matchEducation = !education || normalizeText(talent.education) === education
    const matchSkill = !skill || skills.some(item => item.includes(skill))

    return matchKeyword && matchCity && matchEducation && matchSkill
  })
}

export function buildTalentFilterOptions(talents) {
  const cities = new Set()
  const educations = new Set()
  const skillCountMap = new Map()

  talents.forEach((talent) => {
    const city = String(talent.city || '').trim()
    const education = String(talent.education || '').trim()

    if (city) cities.add(city)
    if (education) educations.add(education)

    splitTalentSkills(talent.skills).forEach((skill) => {
      skillCountMap.set(skill, (skillCountMap.get(skill) || 0) + 1)
    })
  })

  const topSkills = [...skillCountMap.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'zh-CN'))
    .slice(0, 8)
    .map(([skill]) => skill)

  return {
    cities: [...cities].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    educations: [...educations].sort((a, b) => a.localeCompare(b, 'zh-CN')),
    skills: topSkills,
  }
}
