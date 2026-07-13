import { AuthLayout } from '../auth-layout'
import { SignUpForm } from './components/sign-up-form'

export function SignUp() {
  return (
    <AuthLayout>
      <SignUpForm className='w-full max-w-3xl' />
    </AuthLayout>
  )
}
