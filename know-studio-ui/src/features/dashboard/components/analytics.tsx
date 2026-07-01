import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useTranslation } from 'react-i18next'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import { Bar, BarChart, CartesianGrid, LabelList, XAxis, YAxis } from 'recharts'
import { AnalyticsChart } from './analytics-chart'

const referrersData = [
  { name: 'Direct', value: 512 },
  { name: 'Product Hunt', value: 238 },
  { name: 'Twitter', value: 174 },
  { name: 'Blog', value: 104 },
]

const devicesData = [
  { name: 'Desktop', value: 74 },
  { name: 'Mobile', value: 22 },
  { name: 'Tablet', value: 4 },
]

export function Analytics() {
  const { t } = useTranslation()
  const referrersConfig = {
    value: {
      label: t('dashboard.chart.visitors'),
      color: 'var(--chart-1)',
    },
  } satisfies ChartConfig
  const devicesConfig = {
    value: {
      label: t('dashboard.chart.share'),
      color: 'var(--chart-2)',
    },
  } satisfies ChartConfig

  return (
    <div className='flex flex-col gap-4'>
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.cards.trafficOverview')}</CardTitle>
          <CardDescription>
            {t('dashboard.cards.trafficOverviewDescription')}
          </CardDescription>
        </CardHeader>
        <CardContent className='px-6'>
          <AnalyticsChart />
        </CardContent>
      </Card>
      <div className='grid gap-4 sm:grid-cols-2 lg:grid-cols-4'>
        <Card>
          <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
            <CardTitle className='text-sm font-medium'>
              {t('dashboard.cards.totalClicks')}
            </CardTitle>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              className='h-4 w-4 text-muted-foreground'
            >
              <path d='M3 3v18h18' />
              <path d='M7 15l4-4 4 4 4-6' />
            </svg>
          </CardHeader>
          <CardContent>
            <div className='text-2xl font-bold'>1,248</div>
            <p className='text-xs text-muted-foreground'>
              {t('dashboard.cards.totalClicksDelta')}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
            <CardTitle className='text-sm font-medium'>
              {t('dashboard.cards.uniqueVisitors')}
            </CardTitle>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              className='h-4 w-4 text-muted-foreground'
            >
              <circle cx='12' cy='7' r='4' />
              <path d='M6 21v-2a6 6 0 0 1 12 0v2' />
            </svg>
          </CardHeader>
          <CardContent>
            <div className='text-2xl font-bold'>832</div>
            <p className='text-xs text-muted-foreground'>
              {t('dashboard.cards.uniqueVisitorsDelta')}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
            <CardTitle className='text-sm font-medium'>
              {t('dashboard.cards.bounceRate')}
            </CardTitle>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              className='h-4 w-4 text-muted-foreground'
            >
              <path d='M3 12h6l3 6 3-6h6' />
            </svg>
          </CardHeader>
          <CardContent>
            <div className='text-2xl font-bold'>42%</div>
            <p className='text-xs text-muted-foreground'>
              {t('dashboard.cards.bounceRateDelta')}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
            <CardTitle className='text-sm font-medium'>
              {t('dashboard.cards.avgSession')}
            </CardTitle>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              className='h-4 w-4 text-muted-foreground'
            >
              <circle cx='12' cy='12' r='10' />
              <path d='M12 6v6l4 2' />
            </svg>
          </CardHeader>
          <CardContent>
            <div className='text-2xl font-bold'>3m 24s</div>
            <p className='text-xs text-muted-foreground'>
              {t('dashboard.cards.avgSessionDelta')}
            </p>
          </CardContent>
        </Card>
      </div>
      <div className='grid grid-cols-1 gap-4 lg:grid-cols-7'>
        <Card className='col-span-1 lg:col-span-4'>
          <CardHeader>
            <CardTitle>{t('dashboard.cards.referrers')}</CardTitle>
            <CardDescription>
              {t('dashboard.cards.referrersDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <MetricsBarChart
              data={referrersData}
              config={referrersConfig}
              valueFormatter={(n) => `${n}`}
            />
          </CardContent>
        </Card>
        <Card className='col-span-1 lg:col-span-3'>
          <CardHeader>
            <CardTitle>{t('dashboard.cards.devices')}</CardTitle>
            <CardDescription>
              {t('dashboard.cards.devicesDescription')}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <MetricsBarChart
              data={devicesData}
              config={devicesConfig}
              valueFormatter={(n) => `${n}%`}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function MetricsBarChart({
  data,
  config,
  valueFormatter,
}: {
  data: { name: string; value: number }[]
  config: ChartConfig
  valueFormatter: (n: number) => string
}) {
  return (
    <ChartContainer config={config} className='h-[220px] w-full'>
      <BarChart
        accessibilityLayer
        data={data}
        layout='vertical'
        margin={{ right: 48 }}
      >
        <CartesianGrid horizontal={false} />
        <YAxis
          dataKey='name'
          type='category'
          tickLine={false}
          tickMargin={10}
          axisLine={false}
          width={90}
        />
        <XAxis dataKey='value' type='number' hide />
        <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
        <Bar dataKey='value' fill='var(--color-value)' radius={10}>
          <LabelList
            dataKey='value'
            position='right'
            offset={8}
            className='fill-foreground'
            fontSize={12}
            formatter={(value) =>
              typeof value === 'number' ? valueFormatter(value) : value
            }
          />
        </Bar>
      </BarChart>
    </ChartContainer>
  )
}
