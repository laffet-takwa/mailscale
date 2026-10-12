import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { cx } from '../lib/utils'
import { useAuth } from '../lib/useAuth'
import { Button } from './ui'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/contacts', label: 'Contacts' },
  { to: '/segments', label: 'Segments' },
  { to: '/templates', label: 'Templates' },
  { to: '/campaigns', label: 'Campaigns' },
  { to: '/emails', label: 'Email Logs' },
  { to: '/suppressions', label: 'Suppressions' },
  { to: '/ai-assistant', label: 'AI Assistant' },
  { to: '/api-keys', label: 'API Keys' },
  { to: '/settings', label: 'Settings' },
  { to: '/monitoring', label: 'System Health' },
]

/** Application shell: sidebar navigation, header, and main region. */
export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-gray-50">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded focus:bg-white focus:p-2"
      >
        Skip to content
      </a>
      <div className="flex">
        <aside className="fixed inset-y-0 w-60 border-r border-gray-200 bg-white">
          <div className="flex h-16 items-center border-b border-gray-200 px-6">
            <span className="text-lg font-semibold text-navy-900">
              MailScale
            </span>
          </div>
          <nav className="mt-4 space-y-1 px-3" aria-label="Main">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cx(
                    'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-navy-50 text-navy-800'
                      : 'text-gray-700 hover:bg-gray-50'
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <div className="ml-60 flex-1">
          <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-8">
            <div />
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">
                {user?.organizationSlug ?? ''}
              </span>
              <Button
                variant="ghost"
                onClick={() => {
                  logout()
                  navigate('/login')
                }}
              >
                Sign out
              </Button>
            </div>
          </header>
          <main id="main-content" className="p-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
