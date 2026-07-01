import * as React from 'react'
import { EyeIcon, EyeOffIcon } from 'lucide-react'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from './ui/input-group'

type PasswordInputProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  'type'
> & {
  ref?: React.Ref<HTMLInputElement>
}

export function PasswordInput({
  className,
  disabled,
  ref,
  ...props
}: PasswordInputProps) {
  const [showPassword, setShowPassword] = React.useState(false)

  return (
    <InputGroup className={className}>
      <InputGroupInput
        type={showPassword ? 'text' : 'password'}
        ref={ref}
        disabled={disabled}
        {...props}
      />
      <InputGroupAddon align='inline-end'>
        <InputGroupButton
          size='icon-xs'
          disabled={disabled}
          onClick={() => setShowPassword((prev) => !prev)}
        >
          {showPassword ? <EyeIcon /> : <EyeOffIcon />}
          <span className='sr-only'>
            {showPassword ? 'Hide password' : 'Show password'}
          </span>
        </InputGroupButton>
      </InputGroupAddon>
    </InputGroup>
  )
}
