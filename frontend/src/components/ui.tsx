import { forwardRef } from 'react'
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'
import { cx } from '../lib/utils'

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    'bg-navy-700 text-white hover:bg-navy-800 focus-visible:ring-navy-500',
  secondary:
    'bg-white text-charcoal-900 border border-gray-300 hover:bg-gray-50 focus-visible:ring-navy-500',
  danger:
    'bg-red-600 text-white hover:bg-red-700 focus-visible:ring-red-500',
  ghost:
    'bg-transparent text-navy-700 hover:bg-navy-50 focus-visible:ring-navy-500',
}

const BASES =
  'inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium ' +
  'transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 ' +
  'disabled:cursor-not-allowed disabled:opacity-50'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  isLoading?: boolean
  children: ReactNode
}

/** Accessible button with a consistent visual system. */
export function Button({
  variant = 'primary',
  isLoading = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      className={cx(BASES, VARIANTS[variant], className)}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      {...rest}
    >
      {isLoading ? (
        <>
          <span
            className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
            aria-hidden="true"
          />
          Loading...
        </>
      ) : (
        children
      )}
    </button>
  )
}

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

/** Labelled input with error display and aria wiring. */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  function Input({ label, error, className, id, ...rest }, ref) {
    const inputId = id ?? rest.name
    const errorId = error && inputId ? `${inputId}-error` : undefined
    return (
      <div className="space-y-1">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-medium text-charcoal-900"
          >
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={cx(
            'block w-full rounded-md border-gray-300 shadow-sm',
            'focus:border-navy-500 focus:ring-navy-500 sm:text-sm',
            'disabled:cursor-not-allowed disabled:bg-gray-100',
            error && 'border-red-500 focus:border-red-500 focus:ring-red-500',
            className
          )}
          aria-invalid={error ? true : undefined}
          aria-describedby={errorId}
          {...rest}
        />
        {error && (
          <p id={errorId} className="text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
      </div>
    )
  }
)

export interface TextareaProps
  extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
}

/** Labelled textarea with error display. */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  function Textarea({ label, error, className, id, ...rest }, ref) {
    const fieldId = id ?? rest.name
    const errorId = error && fieldId ? `${fieldId}-error` : undefined
    return (
      <div className="space-y-1">
        {label && (
          <label
            htmlFor={fieldId}
            className="block text-sm font-medium text-charcoal-900"
          >
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={fieldId}
          className={cx(
            'block w-full rounded-md border-gray-300 shadow-sm',
            'focus:border-navy-500 focus:ring-navy-500 sm:text-sm',
            error && 'border-red-500',
            className
          )}
          aria-invalid={error ? true : undefined}
          aria-describedby={errorId}
          {...rest}
        />
        {error && (
          <p id={errorId} className="text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
      </div>
    )
  }
)

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: Array<{ value: string | number; label: string }>
}

/** Labelled select with option list. */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  function Select({ label, error, options, className, id, ...rest }, ref) {
    const fieldId = id ?? rest.name
    const errorId = error && fieldId ? `${fieldId}-error` : undefined
    return (
      <div className="space-y-1">
        {label && (
          <label
            htmlFor={fieldId}
            className="block text-sm font-medium text-charcoal-900"
          >
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={fieldId}
          className={cx(
            'block w-full rounded-md border-gray-300 shadow-sm',
            'focus:border-navy-500 focus:ring-navy-500 sm:text-sm',
            error && 'border-red-500',
            className
          )}
          aria-invalid={error ? true : undefined}
          aria-describedby={errorId}
          {...rest}
        >
          <option value="">Select...</option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {error && (
          <p id={errorId} className="text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
      </div>
    )
  }
)

export type BadgeTone =
  | 'neutral'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'

const TONES: Record<BadgeTone, string> = {
  neutral: 'bg-gray-100 text-gray-700',
  success: 'bg-green-100 text-green-800',
  warning: 'bg-amber-100 text-amber-800',
  danger: 'bg-red-100 text-red-800',
  info: 'bg-navy-100 text-navy-800',
}

export interface BadgeProps {
  tone?: BadgeTone
  children: ReactNode
}

/** Status pill used for message/campaign/contact states. */
export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return (
    <span
      className={cx(
        'inline-flex items-center rounded-full px-2.5 py-0.5',
        'text-xs font-medium',
        TONES[tone]
      )}
    >
      {children}
    </span>
  )
}

export interface SpinnerProps {
  label?: string
}

/** Loading indicator with accessible text. */
export function Spinner({ label = 'Loading...' }: SpinnerProps) {
  return (
    <div role="status" className="flex items-center justify-center py-12">
      <span
        className="mr-3 h-8 w-8 animate-spin rounded-full border-4 border-navy-500 border-t-transparent"
        aria-hidden="true"
      />
      <span className="text-sm text-gray-600">{label}</span>
    </div>
  )
}

export interface AlertProps {
  tone: 'info' | 'success' | 'warning' | 'danger'
  title?: string
  children: ReactNode
}

/** Inline alert box with role="alert". */
export function Alert({ tone, title, children }: AlertProps) {
  const tones: Record<AlertProps['tone'], string> = {
    info: 'bg-navy-50 text-navy-900 border-navy-200',
    success: 'bg-green-50 text-green-900 border-green-200',
    warning: 'bg-amber-50 text-amber-900 border-amber-200',
    danger: 'bg-red-50 text-red-900 border-red-200',
  }
  return (
    <div
      className={cx('rounded-md border p-4 text-sm', tones[tone])}
      role="alert"
    >
      {title && <p className="font-medium">{title}</p>}
      <div className="mt-1">{children}</div>
    </div>
  )
}

export interface CardProps {
  children: ReactNode
  className?: string
}

/** Content container with consistent padding and border. */
export function Card({ children, className }: CardProps) {
  return (
    <div
      className={cx(
        'rounded-lg border border-gray-200 bg-white p-6 shadow-sm',
        className
      )}
    >
      {children}
    </div>
  )
}

export interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
}

/** Empty-data placeholder with optional action. */
export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-200 py-16 text-center">
      <h3 className="text-sm font-medium text-charcoal-900">{title}</h3>
      {description && (
        <p className="mt-1 max-w-sm text-sm text-gray-500">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
