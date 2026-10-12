import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Input, Alert, Card } from '../components/ui'
import { authStore } from '../lib/authStore'
import { errorMessage } from '../lib/utils'

const schema = z
  .object({
    email: z.string().email('Enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
    fullName: z.string().min(2, 'Enter your full name'),
    organizationName: z.string().min(2, 'Enter an organization name'),
    organizationSlug: z
      .string()
      .min(2, 'Slug must be at least 2 characters')
      .regex(
        /^[a-z0-9-]+$/,
        'Use lowercase letters, numbers, and hyphens only'
      ),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type FormValues = z.infer<typeof schema>

/** Registration screen that provisions a new organization. */
export default function RegisterPage() {
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
      await authStore.register(values)
      navigate('/dashboard')
    } catch (error) {
      setSubmitError(errorMessage(error))
    } finally {
      setIsLoading(false)
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold text-navy-900">
            Create your organization
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            Start sending with MailScale
          </p>
        </div>
        <Card>
          <form onSubmit={onSubmit} noValidate className="space-y-4">
            {submitError && (
              <Alert tone="danger" title="Registration failed">
                {submitError}
              </Alert>
            )}
            <Input
              label="Full name"
              autoComplete="name"
              error={errors.fullName?.message}
              {...register('fullName')}
            />
            <Input
              label="Email"
              type="email"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Organization name"
              error={errors.organizationName?.message}
              {...register('organizationName')}
            />
            <Input
              label="Organization slug"
              error={errors.organizationSlug?.message}
              {...register('organizationSlug')}
            />
            <Input
              label="Password"
              type="password"
              autoComplete="new-password"
              error={errors.password?.message}
              {...register('password')}
            />
            <Input
              label="Confirm password"
              type="password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Button type="submit" className="w-full" isLoading={isLoading}>
              Create organization
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-medium text-navy-700 hover:text-navy-800"
            >
              Sign in
            </Link>
          </p>
        </Card>
      </div>
    </div>
  )
}
