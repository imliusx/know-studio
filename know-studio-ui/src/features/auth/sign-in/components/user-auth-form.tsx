import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { DraftingCompass as LuDraftingCompass, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/stores/auth-store'
import { cn } from '@/lib/utils'
import { extractApiError } from '@/api/http'
import { login } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'

type FieldKey = 'loginId' | 'password'
type FieldErrors = Partial<Record<FieldKey, string>>
type Values = Record<FieldKey, string>

interface UserAuthFormProps extends React.ComponentProps<'div'> {
  redirectTo?: string
}

export function UserAuthForm({
  className,
  redirectTo,
  ...props
}: UserAuthFormProps) {
  const [isLoading, setIsLoading] = useState(false)
  const [values, setValues] = useState<Values>({
    loginId: '',
    password: '',
  })
  const [touched, setTouched] = useState<Record<FieldKey, boolean>>({
    loginId: false,
    password: false,
  })
  const [errors, setErrors] = useState<FieldErrors>({})
  const navigate = useNavigate()
  const { auth } = useAuthStore()

  function validate(nextValues: Values): FieldErrors {
    return {
      loginId: nextValues.loginId.trim() ? undefined : '请输入用户名或邮箱',
      password: nextValues.password ? undefined : '请输入密码',
    }
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

    if (touched[field]) {
      setErrors(validate(nextValues))
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const nextErrors = validate(values)
    setErrors(nextErrors)
    setTouched({ loginId: true, password: true })

    if (nextErrors.loginId || nextErrors.password) return

    const loginId = values.loginId.trim()

    setIsLoading(true)

    try {
      const tokens = await login({ loginId, password: values.password })
      auth.setAccessToken(tokens.accessToken)
      auth.setUser(tokens.currentUser)
      toast.success('登录成功')
      const target = redirectTo && redirectTo !== '/500' ? redirectTo : '/'
      navigate({ to: target, replace: true })
    } catch (error) {
      toast.error(extractApiError(error, '登录失败'))
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
                  Know Studio
                </h1>
                <p className='text-balance text-muted-foreground'>
                  登录账号继续访问工作台
                </p>
              </div>
              <Field data-invalid={Boolean(fieldError('loginId'))}>
                <FieldLabel htmlFor='loginId'>用户名或邮箱</FieldLabel>
                <Input
                  id='loginId'
                  name='loginId'
                  type='text'
                  placeholder='admin 或 admin@example.com'
                  autoComplete='username'
                  value={values.loginId}
                  onChange={(event) =>
                    handleChange('loginId', event.target.value)
                  }
                  onBlur={() => handleBlur('loginId')}
                  aria-invalid={Boolean(fieldError('loginId'))}
                  disabled={isLoading}
                />
                {fieldError('loginId') ? (
                  <FieldError>{fieldError('loginId')}</FieldError>
                ) : null}
              </Field>
              <Field data-invalid={Boolean(fieldError('password'))}>
                <div className='flex items-center'>
                  <FieldLabel htmlFor='password'>密码</FieldLabel>
                  <Link
                    to='/forgot-password'
                    className='ml-auto text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline'
                  >
                    忘记密码？
                  </Link>
                </div>
                <Input
                  id='password'
                  name='password'
                  type='password'
                  autoComplete='current-password'
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
              <Field>
                <Button type='submit' disabled={isLoading}>
                  {isLoading ? (
                    <Loader2 data-icon='inline-start' className='animate-spin' />
                  ) : null}
                  {isLoading ? '登录中' : '登录'}
                </Button>
              </Field>
              <FieldSeparator className='*:data-[slot=field-separator-content]:bg-popover' />
              <FieldDescription className='text-center'>
                还没有账号？ <Link to='/sign-up'>注册</Link>
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
      <FieldDescription className='px-6 text-center'>
        后端 Refresh Token 使用 HttpOnly Cookie，前端仅保存 Access Token。
      </FieldDescription>
    </div>
  )
}
