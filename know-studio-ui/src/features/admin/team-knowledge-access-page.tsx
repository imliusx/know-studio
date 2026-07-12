import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import {
  listKnowledgeBases,
  listKnowledgeBaseTeamGrants,
  saveKnowledgeBaseTeamGrant,
  type KnowledgeBasePermission,
} from '@/api/knowledge-bases'
import {
  addTeamMember,
  createTeam,
  listTeamMembers,
  listTeams,
  removeTeamMember,
  updateTeamMember,
  type TeamRole,
} from '@/api/teams'
import { extractApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth-store'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

export function TeamKnowledgeAccessPage() {
  const queryClient = useQueryClient()
  const isSystemAdmin = useAuthStore((state) => state.auth.user?.systemRole === 'ADMIN')
  const [selectedTeamId, setSelectedTeamId] = useState('')
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState('')
  const [teamValues, setTeamValues] = useState({ name: '', description: '' })
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<TeamRole>('MEMBER')
  const [grantPermission, setGrantPermission] = useState<KnowledgeBasePermission>('READ')

  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: listTeams })
  const knowledgeBasesQuery = useQuery({ queryKey: ['knowledge-bases'], queryFn: listKnowledgeBases })
  const teams = teamsQuery.data ?? []
  const knowledgeBases = useMemo(
    () => knowledgeBasesQuery.data ?? [],
    [knowledgeBasesQuery.data]
  )
  const effectiveTeamId = Number(selectedTeamId || teams[0]?.teamId || 0)
  const effectiveKnowledgeBaseId = Number(
    selectedKnowledgeBaseId || knowledgeBases.find((item) => item.permission === 'MANAGE')?.knowledgeBaseId || 0
  )
  const selectedTeam = teams.find((team) => team.teamId === effectiveTeamId)
  const manageableKnowledgeBases = useMemo(
    () => knowledgeBases.filter((item) => item.permission === 'MANAGE'),
    [knowledgeBases]
  )

  const membersQuery = useQuery({
    queryKey: ['teams', effectiveTeamId, 'members'],
    queryFn: () => listTeamMembers(effectiveTeamId),
    enabled: effectiveTeamId > 0 && selectedTeam?.role === 'TEAM_ADMIN',
  })
  const grantsQuery = useQuery({
    queryKey: ['knowledge-base-grants', effectiveKnowledgeBaseId],
    queryFn: () => listKnowledgeBaseTeamGrants(effectiveKnowledgeBaseId),
    enabled: effectiveKnowledgeBaseId > 0,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['teams'] })
    queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] })
    queryClient.invalidateQueries({ queryKey: ['knowledge-base-grants'] })
  }
  const onError = (error: unknown) => toast.error(extractApiError(error, '操作失败'))
  const createMutation = useMutation({
    mutationFn: createTeam,
    onSuccess: () => {
      setTeamValues({ name: '', description: '' })
      toast.success('团队已创建')
      refresh()
    },
    onError,
  })
  const memberMutation = useMutation({
    mutationFn: () => addTeamMember(effectiveTeamId, { email: memberEmail.trim(), role: memberRole }),
    onSuccess: () => {
      setMemberEmail('')
      toast.success('成员已添加')
      queryClient.invalidateQueries({ queryKey: ['teams', effectiveTeamId, 'members'] })
    },
    onError,
  })
  const grantMutation = useMutation({
    mutationFn: () => saveKnowledgeBaseTeamGrant(effectiveKnowledgeBaseId, effectiveTeamId, grantPermission),
    onSuccess: () => {
      toast.success('知识库授权已更新')
      queryClient.invalidateQueries({ queryKey: ['knowledge-base-grants', effectiveKnowledgeBaseId] })
    },
    onError,
  })

  function submitTeam(event: FormEvent) {
    event.preventDefault()
    if (!teamValues.name.trim()) return
    createMutation.mutate({ name: teamValues.name.trim(), description: teamValues.description.trim() || undefined })
  }

  return (
    <>
      <Header fixed><HeaderActions /></Header>
      <Main className='flex flex-col gap-6 pt-4'>
        <div className='flex items-end justify-between gap-3'>
          <h1 className='text-2xl font-bold'>团队与知识库授权</h1>
          <Button variant='outline' size='sm' onClick={refresh}><RefreshCw />刷新</Button>
        </div>
        {isSystemAdmin ? (
          <Card>
            <CardHeader><CardTitle>创建团队</CardTitle></CardHeader>
            <CardContent>
              <form className='grid gap-3 md:grid-cols-[1fr_2fr_auto]' onSubmit={submitTeam}>
                <Input placeholder='团队名称' value={teamValues.name} onChange={(event) => setTeamValues((value) => ({ ...value, name: event.target.value }))} />
                <Input placeholder='描述' value={teamValues.description} onChange={(event) => setTeamValues((value) => ({ ...value, description: event.target.value }))} />
                <Button type='submit' disabled={createMutation.isPending}>创建</Button>
              </form>
            </CardContent>
          </Card>
        ) : null}
        <div className='grid gap-6 xl:grid-cols-2'>
          <Card>
            <CardHeader><CardTitle>团队成员</CardTitle></CardHeader>
            <CardContent className='space-y-4'>
              <Select value={String(effectiveTeamId || '')} onValueChange={setSelectedTeamId}>
                <SelectTrigger><SelectValue placeholder='选择团队' /></SelectTrigger>
                <SelectContent>{teams.map((team) => <SelectItem key={team.teamId} value={String(team.teamId)}>{team.name}</SelectItem>)}</SelectContent>
              </Select>
              {selectedTeam?.role === 'TEAM_ADMIN' ? (
                <div className='grid gap-2 md:grid-cols-[1fr_140px_auto]'>
                  <Input placeholder='成员邮箱' value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} />
                  <Select value={memberRole} onValueChange={(value) => setMemberRole(value as TeamRole)}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value='MEMBER'>MEMBER</SelectItem><SelectItem value='TEAM_ADMIN'>TEAM_ADMIN</SelectItem></SelectContent></Select>
                  <Button disabled={!memberEmail.trim() || memberMutation.isPending} onClick={() => memberMutation.mutate()}>添加</Button>
                </div>
              ) : null}
              <Table><TableHeader><TableRow><TableHead>成员</TableHead><TableHead>角色</TableHead><TableHead className='w-20' /></TableRow></TableHeader><TableBody>
                {(membersQuery.data ?? []).map((member) => <TableRow key={member.userId}><TableCell><div>{member.displayName}</div><div className='text-xs text-muted-foreground'>{member.email}</div></TableCell><TableCell><Select value={member.role} onValueChange={(role) => updateTeamMember(effectiveTeamId, member.userId, role as TeamRole).then(() => queryClient.invalidateQueries({ queryKey: ['teams', effectiveTeamId, 'members'] })).catch(onError)}><SelectTrigger className='w-36'><SelectValue /></SelectTrigger><SelectContent><SelectItem value='MEMBER'>MEMBER</SelectItem><SelectItem value='TEAM_ADMIN'>TEAM_ADMIN</SelectItem></SelectContent></Select></TableCell><TableCell><Button variant='ghost' size='icon' title='移除成员' onClick={() => removeTeamMember(effectiveTeamId, member.userId).then(() => queryClient.invalidateQueries({ queryKey: ['teams', effectiveTeamId, 'members'] })).catch(onError)}><Trash2 /></Button></TableCell></TableRow>)}
              </TableBody></Table>
            </CardContent>
          </Card>
          <Card>
            <CardHeader><CardTitle>知识库 Team Grant</CardTitle></CardHeader>
            <CardContent className='space-y-4'>
              <Select value={String(effectiveKnowledgeBaseId || '')} onValueChange={setSelectedKnowledgeBaseId}><SelectTrigger><SelectValue placeholder='选择可管理知识库' /></SelectTrigger><SelectContent>{manageableKnowledgeBases.map((item) => <SelectItem key={item.knowledgeBaseId} value={String(item.knowledgeBaseId)}>{item.name}</SelectItem>)}</SelectContent></Select>
              <div className='grid gap-2 md:grid-cols-[1fr_140px_auto]'>
                <Select value={String(effectiveTeamId || '')} onValueChange={setSelectedTeamId}><SelectTrigger><SelectValue placeholder='选择团队' /></SelectTrigger><SelectContent>{teams.map((team) => <SelectItem key={team.teamId} value={String(team.teamId)}>{team.name}</SelectItem>)}</SelectContent></Select>
                <Select value={grantPermission} onValueChange={(value) => setGrantPermission(value as KnowledgeBasePermission)}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value='READ'>READ</SelectItem><SelectItem value='MANAGE'>MANAGE</SelectItem></SelectContent></Select>
                <Button disabled={!effectiveTeamId || !effectiveKnowledgeBaseId || grantMutation.isPending} onClick={() => grantMutation.mutate()}>保存</Button>
              </div>
              <Table><TableHeader><TableRow><TableHead>团队</TableHead><TableHead>权限</TableHead></TableRow></TableHeader><TableBody>{(grantsQuery.data ?? []).map((grant) => <TableRow key={grant.teamId}><TableCell>{teams.find((team) => team.teamId === grant.teamId)?.name ?? grant.teamId}</TableCell><TableCell>{grant.permission}</TableCell></TableRow>)}</TableBody></Table>
            </CardContent>
          </Card>
        </div>
      </Main>
    </>
  )
}
