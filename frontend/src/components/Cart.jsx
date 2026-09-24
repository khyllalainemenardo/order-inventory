import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Cart({ stock, onPlaced }) {
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [lines, setLines] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!productId && stock.length > 0) setProductId(stock[0].productId)
  }, [stock, productId])

  const nameOf = (id) => stock.find((item) => item.productId === id)?.name ?? id

  function addLine(event) {
    event.preventDefault()
    const qty = Number(quantity)
    if (!productId || !Number.isInteger(qty) || qty < 1) return
    setLines((current) => {
      const existing = current.find((line) => line.productId === productId)
      return existing
        ? current.map((line) =>
            line.productId === productId ? { ...line, quantity: line.quantity + qty } : line
          )
        : [...current, { productId, quantity: qty }]
    })
    setQuantity(1)
  }

  function changeQuantity(id, delta) {
    setLines((current) =>
      current.map((line) =>
        line.productId === id ? { ...line, quantity: Math.max(1, line.quantity + delta) } : line
      )
    )
  }

  function removeLine(id) {
    setLines((current) => current.filter((line) => line.productId !== id))
  }

  async function placeOrder() {
    setSending(true)
    setError(null)
    try {
      const order = await api.placeOrder(lines)
      setResult({ ...order, at: Date.now() })
      if (order.status === 'CONFIRMED') setLines([])
      await onPlaced()
    } catch (e) {
      setResult(null)
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  const units = lines.reduce((sum, line) => sum + line.quantity, 0)

  return (
    <section className="order" aria-labelledby="order-heading">
      <h2 id="order-heading">Cart</h2>

      <form onSubmit={addLine} className="picker">
        <label htmlFor="product">Product</label>
        <select id="product" value={productId} onChange={(e) => setProductId(e.target.value)}>
          {stock.length === 0 && <option value="">Loading stock…</option>}
          {stock.map((item) => (
            <option key={item.productId} value={item.productId}>
              {item.name} ({item.stock} in stock)
            </option>
          ))}
        </select>

        <label htmlFor="quantity">Quantity</label>
        <div className="add-row">
          <input
            id="quantity"
            type="number"
            min="1"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
          />
          <button type="submit" className="secondary" disabled={!productId}>
            Add to cart
          </button>
        </div>
      </form>

      {lines.length === 0 ? (
        <p className="hint">The cart is empty. Add a product to start an order.</p>
      ) : (
        <ul className="cart-lines">
          {lines.map((line) => (
            <li key={line.productId}>
              <span className="line-name">{nameOf(line.productId)}</span>
              <span className="stepper">
                <button
                  type="button"
                  aria-label={`One fewer ${nameOf(line.productId)}`}
                  onClick={() => changeQuantity(line.productId, -1)}
                  disabled={line.quantity <= 1}
                >
                  −
                </button>
                <span className="line-qty">{line.quantity}</span>
                <button
                  type="button"
                  aria-label={`One more ${nameOf(line.productId)}`}
                  onClick={() => changeQuantity(line.productId, 1)}
                >
                  +
                </button>
              </span>
              <button type="button" className="link" onClick={() => removeLine(line.productId)}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}

      <button
        type="button"
        className="place"
        onClick={placeOrder}
        disabled={sending || lines.length === 0}
      >
        {sending
          ? 'Placing order…'
          : lines.length === 0
            ? 'Place order'
            : `Place order (${units} ${units === 1 ? 'unit' : 'units'})`}
      </button>

      {error && <p className="notice">{error}</p>}
      {result && <Slip key={result.at} result={result} nameOf={nameOf} />}
    </section>
  )
}

const OUTCOME_TEXT = {
  RESERVED: 'Reserved',
  INSUFFICIENT_STOCK: 'Not enough stock',
  NOT_RESERVED: 'Not reserved'
}

function Slip({ result, nameOf }) {
  const confirmed = result.status === 'CONFIRMED'
  return (
    <article className={confirmed ? 'slip yes' : 'slip no'}>
      <p className="stamp">{result.status}</p>
      <p className="verdict">
        {confirmed ? 'Every line was reserved.' : result.reason}
      </p>
      <ul className="outcomes">
        {result.items.map((item) => (
          <li key={item.productId} className={`outcome ${item.outcome.toLowerCase()}`}>
            <span>
              {item.quantity} × {nameOf(item.productId)}
            </span>
            <span>{OUTCOME_TEXT[item.outcome] ?? item.outcome}</span>
          </li>
        ))}
      </ul>
    </article>
  )
}
