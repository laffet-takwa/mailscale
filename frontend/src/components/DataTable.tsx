import type { ReactNode } from 'react'
import { cx } from '../lib/utils'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  className?: string
}

export interface DataTableProps<T> {
  columns: Array<Column<T>>
  rows: T[]
  keyFn: (row: T) => string | number
  emptyMessage?: string
  className?: string
}

/**
 * Accessible data table: caption-less header row, scoped cells,
 * and a configurable empty state.
 */
export function DataTable<T>({
  columns,
  rows,
  keyFn,
  emptyMessage = 'No records found',
  className,
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return (
      <div className="rounded-lg border-2 border-dashed border-gray-200 py-12 text-center">
        <p className="text-sm text-gray-500">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className={cx('overflow-x-auto', className)}>
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cx(
                  'px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500',
                  column.className
                )}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200 bg-white">
          {rows.map((row) => (
            <tr key={keyFn(row)} className="hover:bg-gray-50">
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={cx(
                    'whitespace-nowrap px-6 py-4 text-sm text-charcoal-900',
                    column.className
                  )}
                >
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
