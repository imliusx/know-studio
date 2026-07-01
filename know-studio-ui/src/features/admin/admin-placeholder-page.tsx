import { Telescope, type LucideIcon } from 'lucide-react'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'

type AdminPlaceholderPageProps = {
  title: string
  description: string
  icon: LucideIcon
  items: string[]
}

export function AdminPlaceholderPage(_props: AdminPlaceholderPageProps) {
  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main fixed className='justify-center'>
        <div className='m-auto flex w-full flex-col items-center justify-center gap-2'>
          <Telescope size={72} />
          <h1 className='text-4xl leading-tight font-bold'>Coming Soon!</h1>
          <p className='text-center text-muted-foreground'>
            This page has not been created yet. <br />
            Stay tuned though!
          </p>
        </div>
      </Main>
    </>
  )
}
