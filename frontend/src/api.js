const API = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'


export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API}${path}`, {
      ...options,
      headers: options.body ? { 'Content-Type': 'application/json' } : undefined
    })
  } catch {
    throw new ApiError(0, 'The order service is not answering. Check that it is running on port 8080 and that CORS allows this origin.')
  }
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(response.status, body?.message ?? `Request failed with ${response.status}.`)
  }
  return body
}

export const api = {
  inventory: () => request('/api/inventory'),
  orders: () => request('/api/orders'),
  notifications: () => request('/api/notifications'),
  placeOrder: (items) =>
    request('/api/orders', { method: 'POST', body: JSON.stringify({ items }) }),
  cancelOrder: (orderId) =>
    request(`/api/orders/${orderId}/cancel`, { method: 'POST' })
}

export const shortId = (id) => (id ? id.slice(0, 8) : '—')

export const clock = (iso) =>
  new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
