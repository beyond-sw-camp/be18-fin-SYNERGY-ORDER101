import axios from 'axios'

export async function getTopCategories() {
  const { data } = await axios.get('/api/v1/categories/top')
  return data ?? []
}

export async function getChildCategories(parentId) {
  if (!parentId) return []
  const { data } = await axios.get(`/api/v1/categories/${parentId}/children`)
  return data ?? []
}
