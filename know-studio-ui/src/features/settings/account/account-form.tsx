import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { changePassword } from '@/api/auth'
import { extractApiError } from '@/api/http'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'

export function AccountForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const mutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      toast.success('密码已修改')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setError(null)
    },
    onError: (requestError) =>
      toast.error(extractApiError(requestError, '修改密码失败')),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!currentPassword || !newPassword) {
      setError('请输入当前密码和新密码')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('两次输入的新密码不一致')
      return
    }

    mutation.mutate({ currentPassword, newPassword })
  }

  return (
    <form onSubmit={handleSubmit}>
      <FieldGroup>
        <Field data-invalid={Boolean(error && !currentPassword)}>
          <FieldLabel htmlFor='currentPassword'>当前密码</FieldLabel>
          <Input
            id='currentPassword'
            type='password'
            autoComplete='current-password'
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            aria-invalid={Boolean(error && !currentPassword)}
          />
        </Field>
        <Field data-invalid={Boolean(error)}>
          <FieldLabel htmlFor='newPassword'>新密码</FieldLabel>
          <Input
            id='newPassword'
            type='password'
            autoComplete='new-password'
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            aria-invalid={Boolean(error)}
          />
          <FieldDescription>修改后请使用新密码重新登录。</FieldDescription>
        </Field>
        <Field data-invalid={Boolean(error)}>
          <FieldLabel htmlFor='confirmPassword'>确认新密码</FieldLabel>
          <Input
            id='confirmPassword'
            type='password'
            autoComplete='new-password'
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            aria-invalid={Boolean(error)}
          />
          {error ? <FieldError>{error}</FieldError> : null}
        </Field>
        <Button type='submit' disabled={mutation.isPending}>
          {mutation.isPending ? '提交中' : '修改密码'}
        </Button>
      </FieldGroup>
    </form>
  )
}
