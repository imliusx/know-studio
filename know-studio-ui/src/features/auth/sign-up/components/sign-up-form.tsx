import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { DraftingCompass as LuDraftingCompass, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { extractApiError } from '@/api/http'
import { register } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type FieldKey =
  | 'username'
  | 'email'
  | 'displayName'
  | 'password'
  | 'confirmPassword'

type FieldErrors = Partial<Record<FieldKey, string>>

type Values = Record<FieldKey, string>

export function SignUpForm({
  className,
  ...props
}: React.ComponentProps<'div'>) {
  const navigate = useNavigate()
  const [isLoading, setIsLoading] = useState(false)
  const [values, setValues] = useState<Values>({
    username: '',
    email: '',
    displayName: '',
    password: '',
    confirmPassword: '',
  })
  const [touched, setTouched] = useState<Record<FieldKey, boolean>>({
    username: false,
    email: false,
    displayName: false,
    password: false,
    confirmPassword: false,
  })
  const [errors, setErrors] = useState<FieldErrors>({})

  function validate(nextValues: Values): FieldErrors {
    const nextErrors: FieldErrors = {
      username: nextValues.username.trim() ? undefined : '请输入用户名',
      email:
        nextValues.email.trim() && EMAIL_PATTERN.test(nextValues.email)
          ? undefined
          : '请输入有效邮箱',
      displayName: nextValues.displayName.trim() ? undefined : '请输入显示名',
      password:
        nextValues.password.length >= 8 ? undefined : '密码至少 8 位',
    }

    if (!nextValues.confirmPassword) {
      nextErrors.confirmPassword = '请再次输入密码'
    } else if (nextValues.confirmPassword !== nextValues.password) {
      nextErrors.confirmPassword = '两次输入的密码不一致'
    }

    return nextErrors
  }

  function fieldError(field: FieldKey) {
    return touched[field] ? errors[field] : undefined
  }

  function handleBlur(field: FieldKey) {
    setTouched((prev) => ({ ...prev, [field]: true }))
    setErrors(validate(values))
  }

  function handleChange(field: FieldKey, value: string) {
    const nextValues = { ...values, [field]: value }
    setValues(nextValues)

    if (touched[field] || (field === 'password' && touched.confirmPassword)) {
      setErrors(validate(nextValues))
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors = validate(values)
    setErrors(nextErrors)
    setTouched({
      username: true,
      email: true,
      displayName: true,
      password: true,
      confirmPassword: true,
    })

    if (Object.values(nextErrors).some(Boolean)) return

    setIsLoading(true)

    try {
      await register({
        username: values.username.trim(),
        email: values.email.trim(),
        displayName: values.displayName.trim(),
        password: values.password,
      })
      toast.success('注册成功，请登录')
      navigate({ to: '/sign-in', replace: true })
    } catch (error) {
      toast.error(extractApiError(error, '注册失败'))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className={cn('flex flex-col gap-6', className)} {...props}>
      <Card className='overflow-hidden bg-popover/55 p-0 text-popover-foreground shadow-xl ring-1 ring-foreground/10 backdrop-blur-2xl backdrop-saturate-150'>
        <CardContent className='grid p-0 md:grid-cols-2'>
          <form
            className='p-6 md:order-last md:p-8'
            onSubmit={onSubmit}
            noValidate
          >
            <FieldGroup>
              <div className='flex flex-col items-center gap-2 text-center'>
                <h1 className='flex items-center justify-center gap-2 text-2xl font-bold'>
                  <LuDraftingCompass
                    className='size-7 text-primary'
                    aria-hidden='true'
                  />
                  创建账号
                </h1>
                <p className='text-sm text-balance text-muted-foreground'>
                  注册 Know Studio 账号
                </p>
              </div>
              <Field data-invalid={Boolean(fieldError('username'))}>
                <FieldLabel htmlFor='username'>用户名</FieldLabel>
                <Input
                  id='username'
                  name='username'
                  autoComplete='username'
                  value={values.username}
                  onChange={(event) =>
                    handleChange('username', event.target.value)
                  }
                  onBlur={() => handleBlur('username')}
                  aria-invalid={Boolean(fieldError('username'))}
                  disabled={isLoading}
                />
                {fieldError('username') ? (
                  <FieldError>{fieldError('username')}</FieldError>
                ) : null}
              </Field>
              <Field data-invalid={Boolean(fieldError('displayName'))}>
                <FieldLabel htmlFor='displayName'>显示名</FieldLabel>
                <Input
                  id='displayName'
                  name='displayName'
                  value={values.displayName}
                  onChange={(event) =>
                    handleChange('displayName', event.target.value)
                  }
                  onBlur={() => handleBlur('displayName')}
                  aria-invalid={Boolean(fieldError('displayName'))}
                  disabled={isLoading}
                />
                {fieldError('displayName') ? (
                  <FieldError>{fieldError('displayName')}</FieldError>
                ) : null}
              </Field>
              <Field data-invalid={Boolean(fieldError('email'))}>
                <FieldLabel htmlFor='email'>邮箱</FieldLabel>
                <Input
                  id='email'
                  name='email'
                  type='email'
                  placeholder='m@example.com'
                  autoComplete='email'
                  value={values.email}
                  onChange={(event) =>
                    handleChange('email', event.target.value)
                  }
                  onBlur={() => handleBlur('email')}
                  aria-invalid={Boolean(fieldError('email'))}
                  disabled={isLoading}
                />
                {fieldError('email') ? (
                  <FieldError>{fieldError('email')}</FieldError>
                ) : null}
              </Field>
              <FieldGroup className='grid gap-4 md:grid-cols-2'>
                <Field data-invalid={Boolean(fieldError('password'))}>
                  <FieldLabel htmlFor='password'>密码</FieldLabel>
                  <Input
                    id='password'
                    name='password'
                    type='password'
                    autoComplete='new-password'
                    value={values.password}
                    onChange={(event) =>
                      handleChange('password', event.target.value)
                    }
                    onBlur={() => handleBlur('password')}
                    aria-invalid={Boolean(fieldError('password'))}
                    disabled={isLoading}
                  />
                  {fieldError('password') ? (
                    <FieldError>{fieldError('password')}</FieldError>
                  ) : null}
                </Field>
                <Field data-invalid={Boolean(fieldError('confirmPassword'))}>
                  <FieldLabel htmlFor='confirmPassword'>确认密码</FieldLabel>
                  <Input
                    id='confirmPassword'
                    name='confirmPassword'
                    type='password'
                    autoComplete='new-password'
                    value={values.confirmPassword}
                    onChange={(event) =>
                      handleChange('confirmPassword', event.target.value)
                    }
                    onBlur={() => handleBlur('confirmPassword')}
                    aria-invalid={Boolean(fieldError('confirmPassword'))}
                    disabled={isLoading}
                  />
                  {fieldError('confirmPassword') ? (
                    <FieldError>{fieldError('confirmPassword')}</FieldError>
                  ) : null}
                </Field>
              </FieldGroup>
              <Field>
                <Button type='submit' disabled={isLoading}>
                  {isLoading ? (
                    <Loader2 data-icon='inline-start' className='animate-spin' />
                  ) : null}
                  {isLoading ? '注册中' : '注册'}
                </Button>
              </Field>
              <FieldDescription className='text-center'>
                已有账号？ <Link to='/sign-in'>登录</Link>
              </FieldDescription>
            </FieldGroup>
          </form>
          <div className='relative hidden bg-muted md:order-first md:block'>
            <img
              src='/placeholder.svg'
              alt=''
              className='absolute inset-0 h-full w-full object-cover dark:brightness-[0.2] dark:grayscale'
            />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
