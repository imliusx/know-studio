import { AuthLayout } from '../auth-layout'
import { UserAuthForm } from './components/user-auth-form'

export function SignIn2() {
  return (
    <AuthLayout>
      <UserAuthForm className='w-full max-w-3xl' />
    </AuthLayout>
  )
}
