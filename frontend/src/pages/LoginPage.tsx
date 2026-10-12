import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Input, Alert, Card } from '../components/ui'
import { authStore } from '../lib/authStore'
import { errorMessage } from '../lib/utils'

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

type FormValues = z.infer<typeof schema>

/** Login screen with validated credentials. */
export default function LoginPage() {
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    setIsLoading(true)
    try {
      await authStore.login(values)
      navigate('/dashboard')
    } catch (error) {
      setSubmitError(errorMessage(error))
    } finally {
      setIsLoading(false)
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold text-navy-900">MailScale</h1>
          <p className="mt-1 text-sm text-gray-600">
            Sign in to your organization
          </p>
        </div>
        <Card>
          <form onSubmit={onSubmit} noValidate className="space-y-4">
            {submitError && (
              <Alert tone="danger" title="Sign in failed">
                {submitError}
              </Alert>
            )}
            <Input
              label="Email"
              type="email"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button
              type="submit"
              className="w-full"
              isLoading={isLoading}
            >
              Sign in
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-gray-600">
            Need an organization?{' '}
            <Link
              to="/register"
              className="font-medium text-navy-700 hover:text-navy-800"
            >
              Register
            </Link>
          </p>
        </Card>
      </div>
    </div>
  )
}
