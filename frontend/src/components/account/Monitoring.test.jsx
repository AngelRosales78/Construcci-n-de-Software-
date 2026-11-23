import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import api from '../../services/api'
import AdminMonitoringPage from './AdminMonitoringPage'

vi.mock('../../services/api',()=>({default:{get:vi.fn(),patch:vi.fn()}}))
vi.mock('../layout/WorkspaceLayout',()=>({default:({children})=><main>{children}</main>}))
const ai={calls:0,unpriced_calls:0,estimated_costs:[],breakdown:[],generated_at:'2026-11-23T12:00:00Z',explanation:'Uso observado del motor local.'}
beforeEach(()=>{vi.resetAllMocks();localStorage.clear();api.get.mockResolvedValue({data:ai})})
afterEach(cleanup)
const open=()=>render(<MemoryRouter><AdminMonitoringPage/></MemoryRouter>)

it('muestra cero llamadas y coste no calculado sin inventar mediciones',async()=>{
  open()
  await screen.findByText('Aún no hay llamadas en este período')
  expect(screen.getByText('No calculado')).toBeTruthy()
  expect(api.get.mock.calls[0][0]).toContain('/admin/monitoring/ai/?start=')
})

it('muestra distribuciones legibles y cuotas con valores exactos',async()=>{
  open();await screen.findByText('No calculado')
  api.get.mockResolvedValueOnce({data:{total:4,blocked:1,results:[{plan:'free',count:3,percentage:'75.00'},{plan:'pro',count:1,percentage:'25.00'}]}})
  fireEvent.click(screen.getByRole('button',{name:'Planes',exact:true}))
  await screen.findByText('3 cuentas · 75.00 %')
  expect(screen.getByRole('progressbar',{name:'Porcentaje pro'}).getAttribute('value')).toBe('25.00')
  api.get.mockResolvedValue({data:{results:[{provider:'local_ai',version:0,limit:100,used:85,percentage:'85.00',status:'warning',timezone:'UTC',resets_at:'2026-11-24T00:00:00Z'}]}})
  fireEvent.click(screen.getByRole('button',{name:'Presupuestos',exact:true}))
  await screen.findByText('Consumo alto')
  fireEvent.click(screen.getByRole('button',{name:'Ajustar límite'}))
  fireEvent.change(screen.getByLabelText('Llamadas por día'),{target:{value:'120'}})
  api.patch.mockResolvedValue({data:{}})
  fireEvent.click(screen.getByRole('button',{name:'Guardar límite'}))
  await waitFor(()=>expect(api.patch).toHaveBeenCalledWith('/admin/monitoring/budgets/local_ai/',{version:0,daily_limit:120,reset_timezone:'UTC'}))
})

it('rechazo de permisos se muestra como error, sin datos administrativos',async()=>{
  api.get.mockRejectedValue({response:{status:403,data:{detail:'No tienes permiso.'}}})
  open()
  expect((await screen.findByRole('alert')).textContent).toContain('No tienes permiso')
  expect(screen.queryByText('Coste estimado')).toBeNull()
})
