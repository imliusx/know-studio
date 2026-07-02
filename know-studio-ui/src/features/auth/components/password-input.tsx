import { useState, type ComponentProps } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from '@/components/ui/input-group'

type PasswordInputProps = Omit<ComponentProps<typeof InputGroupInput>, 'type'>

export function PasswordInput({ disabled, ...props }: PasswordInputProps) {
  const [isVisible, setIsVisible] = useState(false)
  const Icon = isVisible ? EyeOff : Eye
  const label = isVisible ? '隐藏密码' : '显示密码'

  return (
    <InputGroup>
      <InputGroupInput
        type={isVisible ? 'text' : 'password'}
        disabled={disabled}
        {...props}
      />
      <InputGroupAddon align='inline-end'>
        <InputGroupButton
          type='button'
          size='icon-xs'
          aria-label={label}
          aria-pressed={isVisible}
          disabled={disabled}
          onClick={() => setIsVisible((current) => !current)}
        >
          <Icon />
          <span className='sr-only'>{label}</span>
        </InputGroupButton>
      </InputGroupAddon>
    </InputGroup>
  )
}
