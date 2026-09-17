import { useState } from 'react'
import { api, clock, shortId } from '../api.js'

export default function OrderHistory({ orders, names, onChanged }) {
  const [busy, setBusy] = useState(null)
  const [errors, setErrors] = useState({})

  async function cancel(orderId) {
    setBusy(orderId)
    setErrors((current) => ({ ...current, [orderId]: null }))
    try {
      await api.cancelOrder(orderId)
      await onChanged()
    } catch (e) {
      setErrors((current) => ({ ...current, [orderId]: e.message }))
    } finally {
      setBusy(null)
    }
  }

  return (
    <section className="history" aria-labelledby="history-heading">
      <div className="section-head">
        <h2 id="history-heading">Orders</h2>
        {orders.length > 0 && <p className="section-note">Newest first</p>}
      </div>

      {orders.length === 0 ? (
        <p className="hint">No orders yet. Placed and rejected orders both show up here.</p>
      ) : (
        <ol className="orders">
          {orders.map((order) => (
            <li key={order.orderId} className={`ticket ${order.status.toLowerCase()}`}>
              <div className="ticket-head">
                <span className="sku" title={order.orderId}>{shortId(order.orderId)}</span>
                <span className="badge">{order.status}</span>
                <time dateTime={order.createdAt}>{clock(order.createdAt)}</time>
                {order.status === 'CONFIRMED' && (
                  <button
                    type="button"
                    className="cancel"
                    onClick={() => cancel(order.orderId)}
                    disabled={busy === order.orderId}
                  >
                    {busy === order.orderId ? 'Cancelling…' : 'Cancel order'}
                  </button>
                )}
              </div>

              <p className="ticket-lines">
                {order.items
                  .map((item) => `${item.quantity} × ${names[item.productId] ?? item.productId}`)
                  .join(', ')}
              </p>

              {order.status === 'REJECTED' && order.reason && (
                <p className="ticket-reason">{order.reason}</p>
              )}
              {order.status === 'CANCELLED' && (
                <p className="ticket-reason">Cancelled. The stock went back on the shelf.</p>
              )}
              {errors[order.orderId] && <p className="notice">{errors[order.orderId]}</p>}
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
