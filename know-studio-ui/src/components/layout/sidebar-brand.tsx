import { DraftingCompass } from 'lucide-react'
import {
  SidebarMenu,
  SidebarMenuItem,
} from '@/components/ui/sidebar'

export function SidebarBrand() {
  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <div className='flex h-11 items-center gap-2 px-2'>
          <div className='flex aspect-square size-8 shrink-0 items-center justify-center text-primary'>
            <DraftingCompass className='size-7' aria-hidden='true' />
          </div>
          <div className='min-w-0 flex-1 group-data-[collapsible=icon]:hidden'>
            <span className='block truncate font-semibold'>KnowStudio</span>
          </div>
        </div>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
