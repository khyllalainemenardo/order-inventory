export default function Shelf({ stock }) {
  const flagged = stock.filter((item) => item.lowStock).length

  return (
    <section className="ledger" aria-labelledby="ledger-heading">
      <div className="section-head">
        <h2 id="ledger-heading">On the shelf</h2>
        {flagged > 0 && (
          <p className="section-note">
            {flagged} {flagged === 1 ? 'product needs' : 'products need'} reordering
          </p>
        )}
      </div>

      <div className="table-wrap">
        <table className="shelf">
          <thead>
            <tr>
              <th scope="col">Product</th>
              <th scope="col" className="num">In stock</th>
            </tr>
          </thead>
          <tbody>
            {stock.length === 0 && (
              <tr>
                <td colSpan={2} className="empty-cell">Loading stock…</td>
              </tr>
            )}
            {stock.map((item) => (
              <tr key={item.productId} className={rowClass(item)}>
                <td>
                  <span className="sku">{item.productId}</span>
                  <span className="product">{item.name}</span>
                  {item.lowStock && (
                    <span className="flag">{item.stock === 0 ? 'Out of stock' : 'Reorder soon'}</span>
                  )}
                </td>
                <td className="num count">{item.stock}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function rowClass(item) {
  if (item.stock === 0) return 'out'
  if (item.lowStock) return 'low'
  return undefined
}
