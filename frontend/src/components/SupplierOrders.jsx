import { clock } from '../api.js'

export default function SupplierOrders({ orders, names }) {
  return (
    <section className="ledger" aria-labelledby="supplier-heading">
      <div className="section-head">
        <h2 id="supplier-heading">Supplier reorders</h2>
        {orders.length > 0 && <p className="section-note">Newest first</p>}
      </div>

      {orders.length === 0 ? (
        <p className="hint">No reorders yet. One is sent when a product drops below the low-stock threshold.</p>
      ) : (
        <div className="table-wrap">
          <table className="shelf">
            <thead>
              <tr>
                <th scope="col">Reorder</th>
                <th scope="col">Product</th>
                <th scope="col" className="num">Units</th>
                <th scope="col">Status</th>
                <th scope="col">Updated</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.reference}>
                  <td><span className="sku">{order.reference}</span></td>
                  <td>{names[order.productId] ?? order.productId}</td>
                  <td className="num">{order.units}</td>
                  <td>{order.status}</td>
                  <td><time dateTime={order.updatedAt}>{clock(order.updatedAt)}</time></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
