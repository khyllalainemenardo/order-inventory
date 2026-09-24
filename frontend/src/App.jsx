import { useEffect, useState } from 'react'
import { api } from './api.js'
import Shelf from './components/Shelf.jsx'
import Cart from './components/Cart.jsx'
import OrderHistory from './components/OrderHistory.jsx'
import ActivityFeed from './components/ActivityFeed.jsx'
import SupplierOrders from './components/SupplierOrders.jsx'

export default function App() {
  const [stock, setStock] = useState([])
  const [orders, setOrders] = useState([])
  const [feed, setFeed] = useState([])
  const [reorders, setReorders] = useState([])
  const [offline, setOffline] = useState(false)

  async function refreshAll() {
    try {
      const [items, history, notifications, supplierOrders] = await Promise.all([
        api.inventory(),
        api.orders(),
        api.notifications(),
        api.supplierOrders()
      ])
      setStock(items)
      setOrders(history)
      setFeed(notifications)
      setReorders(supplierOrders)
      setOffline(false)
    } catch {
      setOffline(true)
    }
  }

  useEffect(() => {
    refreshAll()
    const timer = setInterval(refreshAll, 10000)
    return () => clearInterval(timer)
  }, [])

  const names = Object.fromEntries(stock.map((item) => [item.productId, item.name]))

  return (
    <div className="desk">
      <header className="masthead">
        <h1>Order desk</h1>
        <p>
          Fill a cart and place it as one order. Every line is checked against the
          shelf first, and if one line is short, nothing is reserved.
        </p>
      </header>

      {offline && (
        <p className="notice banner">
          The order service is not answering. Start the Spring Boot app on port
          8080, then reload.
        </p>
      )}

      <main className="counter">
        <div className="main-column">
          <Shelf stock={stock} />
          <OrderHistory orders={orders} names={names} onChanged={refreshAll} />
          <SupplierOrders orders={reorders} names={names} />
        </div>

        <aside className="side-column">
          <Cart stock={stock} onPlaced={refreshAll} />
          <ActivityFeed entries={feed} />
        </aside>
      </main>
    </div>
  )
}
