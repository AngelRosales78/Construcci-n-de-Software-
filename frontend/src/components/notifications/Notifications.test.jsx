import React from 'react'
import { afterEach,beforeEach,expect,it,vi } from 'vitest'
import { act,cleanup,fireEvent,render,screen,waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotificationsPage from './NotificationsPage'
import NotificationBell from './NotificationBell'
import NotificationPreferencesPage from './NotificationPreferencesPage'
import api from '../../services/api'
import { saveSession } from '../../services/sessionStore'

vi.mock('../../services/api',()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn()}}))
vi.mock('../layout/WorkspaceLayout',()=>({default:({children})=><main>{children}</main>}))
const detail={as_of:'2024-12-31',source:'Escenario de prueba'}
const item={id:1,title:'Ejemplo de aviso: MSFT',ticker:'MSFT',url:'/company/MSFT',message:'Muy sólido → Moderado. Perfil según Altman-Z.',
  created_at:'2026-11-19T15:00:00Z',read_at:null,is_demo:true,before:detail,after:detail}
const list={owner_id:'9',count:1,results:[item],unread_count:1,read_through:1,next:null,previous:null}
const prefs={owner_id:'9',version:0,rating_changes:true,channels:{web:true,email:false,push:false},
  available_channels:{web:true,email:false,push:false},channel_messages:{email:'Correo de alertas no habilitado.',push:'Pendiente Firebase y Android.'}}
beforeEach(()=>{
  vi.resetAllMocks();localStorage.clear()
  saveSession({access:'access',refresh:'refresh',user:{id:9}})
  api.get.mockImplementation(path=>Promise.resolve({data:path.includes('notification-preferences')?prefs:list}))
})
afterEach(()=>{cleanup();localStorage.clear()})
const open=Page=>render(<MemoryRouter><Page/></MemoryRouter>)

it('identifica ejemplos, muestra procedencia y marca un aviso propio',async()=>{
  api.post.mockImplementation(async()=>{api.get.mockResolvedValue({data:{...list,unread_count:0,results:[{...item,read_at:'2026-11-19T15:01:00Z'}]}});return {data:{owner_id:'9',updated:1,unread_count:0}}})
  open(NotificationsPage)
  expect(await screen.findByText('Ejemplo de aviso: MSFT')).toBeTruthy()
  expect(screen.getByText(/No representa un cambio real/)).toBeTruthy()
  fireEvent.click(screen.getByText('Ver balance y procedencia'))
  expect(screen.getByText('Fuente nueva: Escenario de prueba')).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:'Marcar aviso 1 como leído'}))
  await screen.findByText('0 sin leer')
  expect(api.post).toHaveBeenCalledWith('/notifications/mark-read/',{ids:[1]},{timeout:180000})
})

it('marca todos con el límite de la lista, sin absorber avisos posteriores',async()=>{
  api.post.mockResolvedValue({data:{owner_id:'9',updated:1,unread_count:0}})
  open(NotificationsPage)
  await screen.findByText('1 sin leer')
  fireEvent.click(screen.getByText('Marcar todos como leídos'))
  await waitFor(()=>expect(api.post).toHaveBeenCalledWith('/notifications/mark-read/',{all:true,through_id:1},{timeout:180000}))
})

it('la campana muestra el contador y enlaces al centro y las preferencias',async()=>{
  open(NotificationBell)
  const summary=await screen.findByLabelText('Notificaciones: 1 sin leer')
  fireEvent.click(summary)
  expect(await screen.findByRole('link',{name:'Ver todos los avisos'})).toBeTruthy()
  expect(screen.getByRole('link',{name:'Preferencias'}).getAttribute('href')).toBe('/profile/notifications')
})

it('guarda cambios automáticamente y mantiene Push y correo de alertas deshabilitados',async()=>{
  api.put.mockResolvedValue({data:{...prefs,version:1,channels:{...prefs.channels,web:false}}})
  open(NotificationPreferencesPage)
  expect((await screen.findByLabelText('Push en Android')).disabled).toBe(true)
  expect(screen.getByLabelText('Correo de alertas').disabled).toBe(true)
  fireEvent.click(screen.getByLabelText('En la web'))
  await screen.findByText('Preferencias guardadas.')
  expect(api.put).toHaveBeenCalledWith('/user/notification-preferences/',{
    version:0,rating_changes:true,channels:{web:false,email:false,push:false}},{timeout:180000})
  expect(screen.getByLabelText('En la web').checked).toBe(false)
})

it('no muestra guardado exitoso cuando falla la red y permite reintentar',async()=>{
  api.put.mockRejectedValueOnce(new Error('Red')).mockResolvedValueOnce({data:{...prefs,version:1,rating_changes:false}})
  open(NotificationPreferencesPage)
  fireEvent.click(await screen.findByLabelText('Cambios del perfil Altman-Z'))
  expect(await screen.findByText('Reintentar guardado')).toBeTruthy()
  expect(screen.queryByText('Preferencias guardadas.')).toBeNull()
  fireEvent.click(screen.getByText('Reintentar guardado'))
  await screen.findByText('Preferencias guardadas.')
})

it('un cambio de cuenta oculta los avisos anteriores',async()=>{
  open(NotificationsPage)
  await screen.findByText('Ejemplo de aviso: MSFT')
  api.get.mockResolvedValue({data:{owner_id:'10',count:0,results:[],unread_count:0,read_through:0}})
  await act(async()=>saveSession({access:'other',refresh:'other',user:{id:10}}))
  await screen.findByText('No tienes avisos en esta lista.')
  expect(screen.queryByText('Ejemplo de aviso: MSFT')).toBeNull()
})
