import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SubscriptionPage from '../components/subscription/SubscriptionPage'
import { subscriptionRepository } from '../models/subscriptionRepository'

vi.mock('../models/subscriptionRepository',()=>({subscriptionRepository:{load:vi.fn(),checkout:vi.fn(),cancel:vi.fn(),refreshPayment:vi.fn()}}))
vi.mock('../components/layout/WorkspaceLayout',()=>({default:({children})=><main>{children}</main>}))
const free={effective_plan:'free',active:true,expires_at:null}
const plans={payment_provider:'mercadopago_test',payment:{ready:true},amount:'9.90',currency:'PEN',duration_days:30,features:[]}
const order={id:'operation-1',request_id:'same-request',status:'ready',status_label:'Listo para pagar',currency:'PEN',amount:'9.90',duration_days:30,checkout_url:'https://www.mercadopago.com.pe/checkout/v1/redirect?order_id=ORDTST01PAYMENT'}
beforeEach(()=>{vi.resetAllMocks();subscriptionRepository.load.mockResolvedValue({plans,subscription:free,receipts:[],payment_orders:[]})})
afterEach(cleanup)
const open=()=>render(<MemoryRouter><SubscriptionPage/></MemoryRouter>)
it('explica la configuración pendiente y no ofrece activación directa',async()=>{
 subscriptionRepository.load.mockResolvedValue({plans:{...plans,payment:{ready:false}},subscription:free,receipts:[],payment_orders:[]})
 open()
 const button=await screen.findByRole('button',{name:'Pago aún no disponible'})
 expect(button.disabled).toBe(true)
 expect(screen.getByText('Pago de prueba pendiente de conexión')).toBeTruthy()
 expect(screen.queryByText('Confirmar activación')).toBeNull()
})
it('envía solo el identificador, muestra checkout externo y mantiene Free',async()=>{
 subscriptionRepository.checkout.mockResolvedValue({payment:order,subscription:free})
 open();fireEvent.click(await screen.findByText('Elegir Pro'))
 subscriptionRepository.load.mockResolvedValue({plans,subscription:free,receipts:[],payment_orders:[order]})
 fireEvent.click(screen.getByText('Preparar pago de prueba'))
 const link=await screen.findByRole('link',{name:'Continuar en Mercado Pago ↗'})
 expect(link.getAttribute('href')).toBe(order.checkout_url)
 expect(Object.keys(subscriptionRepository.checkout.mock.calls[0][0])).toEqual(['request_id'])
 expect(screen.getByText('Plan Free')).toBeTruthy()
})
it('muestra pendiente sin activar Pro al consultar el estado',async()=>{
 subscriptionRepository.load.mockResolvedValue({plans,subscription:free,receipts:[],payment_orders:[order]})
 subscriptionRepository.refreshPayment.mockResolvedValue({payment:{...order,status:'pending',status_label:'Pendiente de confirmación'},subscription:free})
 open();fireEvent.click(await screen.findByText('Verificar estado del pago'))
 await screen.findByText('Pendiente de confirmación')
 expect(screen.getByText('Plan Free')).toBeTruthy()
 expect(subscriptionRepository.refreshPayment).toHaveBeenCalledWith(order.id)
})
