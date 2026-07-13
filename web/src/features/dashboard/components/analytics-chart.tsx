import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import { useTranslation } from 'react-i18next'
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'

const data = [
  {
    name: 'Mon',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Tue',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Wed',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Thu',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Fri',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Sat',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
  {
    name: 'Sun',
    clicks: Math.floor(Math.random() * 900) + 100,
    uniques: Math.floor(Math.random() * 700) + 80,
  },
]

export function AnalyticsChart() {
  const { t } = useTranslation()
  const chartConfig = {
    clicks: {
      label: t('dashboard.chart.clicks'),
      color: 'var(--chart-2)',
    },
    uniques: {
      label: t('dashboard.chart.uniqueVisitors'),
      color: 'var(--chart-1)',
    },
  } satisfies ChartConfig

  return (
    <ChartContainer config={chartConfig} className='h-[300px] w-full'>
      <AreaChart accessibilityLayer data={data}>
        <defs>
          <linearGradient id='fillClicks' x1='0' y1='0' x2='0' y2='1'>
            <stop
              offset='5%'
              stopColor='var(--color-clicks)'
              stopOpacity={0.8}
            />
            <stop
              offset='95%'
              stopColor='var(--color-clicks)'
              stopOpacity={0.1}
            />
          </linearGradient>
          <linearGradient id='fillUniques' x1='0' y1='0' x2='0' y2='1'>
            <stop
              offset='5%'
              stopColor='var(--color-uniques)'
              stopOpacity={0.8}
            />
            <stop
              offset='95%'
              stopColor='var(--color-uniques)'
              stopOpacity={0.1}
            />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey='name'
          tickLine={false}
          axisLine={false}
          tickMargin={10}
        />
        <YAxis
          tickLine={false}
          axisLine={false}
          tickMargin={8}
        />
        <ChartTooltip
          cursor={false}
          content={<ChartTooltipContent indicator='line' />}
        />
        <Area
          type='natural'
          dataKey='clicks'
          fill='url(#fillClicks)'
          fillOpacity={0.4}
          stroke='var(--color-clicks)'
          strokeWidth={2}
          stackId='a'
        />
        <Area
          type='natural'
          dataKey='uniques'
          fill='url(#fillUniques)'
          fillOpacity={0.4}
          stroke='var(--color-uniques)'
          strokeWidth={2}
          stackId='a'
        />
        <ChartLegend content={<ChartLegendContent />} />
      </AreaChart>
    </ChartContainer>
  )
}
