import { motion, useReducedMotion } from 'motion/react'
import { cn } from '@/lib/utils'

type MainProps = React.HTMLAttributes<HTMLElement> & {
  fixed?: boolean
  fluid?: boolean
  ref?: React.Ref<HTMLElement>
}

export function Main({
  fixed,
  className,
  fluid,
  children,
  onScroll,
  id = 'content',
  ...props
}: MainProps) {
  const reduceMotion = useReducedMotion()

  function handleScroll(event: React.UIEvent<HTMLElement>) {
    onScroll?.(event)
  }

  return (
    <main
      id={id}
      data-layout={fixed ? 'fixed' : 'auto'}
      data-layout-scroll
      className={cn(
        'min-h-0 flex-1 overflow-y-auto overscroll-contain scroll-smooth peer-data-[fixed-header=true]/header:pt-16',
        fixed && 'overflow-hidden'
      )}
      onScroll={handleScroll}
      {...props}
    >
      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 10, scale: 0.995 }}
        animate={reduceMotion ? undefined : { opacity: 1, y: 0, scale: 1 }}
        transition={
          reduceMotion
            ? undefined
            : { duration: 0.28, ease: [0.22, 1, 0.36, 1] }
        }
        className={cn(
          'px-4 py-6',

          // If layout is fixed, make the content container fill the available
          // height so nested panes can own their own scrolling.
          fixed && 'flex h-full min-h-0 flex-col overflow-hidden',

          // If layout is not fluid, set the max-width
          !fluid &&
            '@7xl/content:mx-auto @7xl/content:w-full @7xl/content:max-w-7xl',
          className
        )}
      >
        {children}
      </motion.div>
    </main>
  )
}
