import { type LucideIcon } from 'lucide-react'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'

type AdminPlaceholderPageProps = {
  title: string
  description: string
  icon: LucideIcon
  items: string[]
}

export function AdminPlaceholderPage({
  title,
  description,
  icon: Icon,
  items,
}: AdminPlaceholderPageProps) {
  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main fixed className='justify-center'>
        <div className='m-auto flex w-full flex-col items-center justify-center gap-2'>
          <Icon size={72} />
          <h1 className='text-4xl leading-tight font-bold'>{title}</h1>
          <p className='max-w-xl text-center text-muted-foreground'>{description}</p>
          <div className='mt-3 flex flex-wrap justify-center gap-2'>
            {items.map((item) => (
              <span key={item} className='rounded border px-3 py-1 text-sm'>
                {item}
              </span>
            ))}
          </div>
        </div>
      </Main>
    </>
  )
}
