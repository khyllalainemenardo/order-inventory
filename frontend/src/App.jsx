import { useEffect, useState } from 'react'

const API = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export default function App() {
  const [stock, setStock] = useState([])
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)
  const [offline, setOffline] = useState(false)

  useEffect(() => {
    loadStock()
  }, [])

  async function loadStock() {
    try {
      const response = await fetch(`${API}/api/inventory`)
      if (!response.ok) throw new Error(response.status)
      const items = await response.json()
      setStock(items)
      setProductId((current) => current || items[0]?.productId || '')
      setOffline(false)
    } catch {
      setOffline(true)
    }
  }

  async function placeOrder(event) {
    event.preventDefault()
    setSending(true)
    try {
      const response = await fetch(`${API}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, quantity: Number(quantity) })
      })
      if (!response.ok) throw new Error(response.status)
      const order = await response.json()
      setResult({ ...order, at: Date.now() })
      setOffline(false)
      if (order.inventory) {
        setStock((items) =>
          items.map((item) =>
            item.productId === order.inventory.productId ? order.inventory : item
          )
        )
      }
    } catch {
      setOffline(true)
      setResult(null)
    } finally {
      setSending(false)
    }
  }

  const selected = stock.find((item) => item.productId === productId)

  return (
    <div className="desk">
      <header className="masthead">
        <h1>Order desk</h1>
        <p>
          Stock is checked and deducted in the same transaction that records the
          order, so the count below is what the database holds right now.
        </p>
      </header>

      <main className="counter">
        <section className="ledger" aria-labelledby="ledger-heading">
          <h2 id="ledger-heading">On the shelf</h2>
          {offline && stock.length === 0 ? (
            <p className="notice">
              The order service is not answering. Start the Spring Boot app on
              port 8080, then reload.
            </p>
          ) : (
            <ul>
              {stock.map((item) => (
                <li key={item.productId} className={item.stock === 0 ? 'row empty' : 'row'}>
                  <span className="sku">{item.productId}</span>
                  <span className="product">{item.name}</span>
                  <span className="count">{item.stock}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="order" aria-labelledby="order-heading">
          <h2 id="order-heading">Place an order</h2>

          <form onSubmit={placeOrder}>
            <label htmlFor="product">Product</label>
            <select
              id="product"
              value={productId}
              onChange={(event) => setProductId(event.target.value)}
            >
              {stock.length === 0 && <option value="">Loading stock…</option>}
              {stock.map((item) => (
                <option key={item.productId} value={item.productId}>
                  {item.name} ({item.stock} in stock)
                </option>
              ))}
            </select>

            <label htmlFor="quantity">Quantity</label>
            <input
              id="quantity"
              type="number"
              min="1"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />

            <button type="submit" disabled={sending || !productId}>
              {sending ? 'Placing…' : 'Place order'}
            </button>
          </form>

          {selected && selected.stock === 0 && (
            <p className="hint">
              {selected.name} is out of stock. Order it anyway to see the
              rejected path.
            </p>
          )}

          {offline && result === null && stock.length > 0 && (
            <p className="notice">
              The request did not reach the server. Check that it is running on
              port 8080 and that CORS allows this origin.
            </p>
          )}

          {result && <Slip key={result.at} result={result} />}
        </section>
      </main>
    </div>
  )
}

function Slip({ result }) {
  const confirmed = result.status === 'CONFIRMED'
  return (
    <article className={confirmed ? 'slip yes' : 'slip no'}>
      <p className="stamp">{result.status}</p>
      <p className="verdict">
        {confirmed
          ? `Reserved. ${result.inventory.name} is down to ${result.inventory.stock}.`
          : result.reason}
      </p>
    </article>
  )
}
