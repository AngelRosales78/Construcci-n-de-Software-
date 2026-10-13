import React from 'react'
import { it, expect, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import PriceChart from './PriceChart'
import api from '../../services/api'
vi.mock('../../services/api',()=>({default:{get:vi.fn()}}))
vi.mock('highcharts/highstock',()=>({default:{}}))
vi.mock('highcharts-react-official',()=>({default:({options})=><div data-testid="chart" data-points={options.series[0].data.length} data-crosshair={options.xAxis.crosshair} data-zoom={options.chart.zooming.type}/>}))
afterEach(()=>{cleanup();vi.resetAllMocks()})
it('muestra cinco métricas, conserva cero y describe los indicadores',()=>{
 render(<MetricCards company={{per:0,roe:-2,market_cap:100,beta:null,dividend_yield:0}}/> )
 expect(screen.getByTitle(/Precio de la acción/)).toBeTruthy()
 expect(screen.getByText('Sin datos')).toBeTruthy()
 expect(screen.getAllByText('0%')).toHaveLength(1)
 expect(screen.getByText('-2%')).toBeTruthy()
})
it('muestra una clasificación explícita en color semántico',()=>{
 render(<FinancialHealthIndicator altmanZ={{zone:'grey',score:1.81}}/> )
 expect(screen.getByText('Moderado')).toBeTruthy()
 expect(screen.getByLabelText('Perfil financiero').className).toContain('orange')
})
it('cambia el rango y entrega más de mil puntos a Highcharts con zoom y crosshair',async()=>{
 const history=Array.from({length:1200},(_,i)=>({date:new Date(Date.UTC(2023,0,i+1)).toISOString().slice(0,10),price:10}))
 api.get.mockResolvedValue({data:{history,sources:['Fixture'],currencies:['USD'],is_demo:true}})
 render(<MemoryRouter initialEntries={['/company/DATA']}><Routes><Route path="/company/:ticker" element={<PriceChart/>}/></Routes></MemoryRouter>)
 await screen.findByTestId('chart')
 expect(screen.getByTestId('chart').getAttribute('data-points')).toBe('1200')
 expect(screen.getByTestId('chart').getAttribute('data-crosshair')).toBe('true')
 expect(screen.getByTestId('chart').getAttribute('data-zoom')).toBe('x')
 fireEvent.click(screen.getByRole('button',{name:'5 años'}))
 await waitFor(()=>expect(api.get).toHaveBeenLastCalledWith('/companies/DATA/history/?range=5Y'))
})
it('explica la falta de precios en vez de generar una serie',async()=>{
 api.get.mockResolvedValue({data:{history:[]}})
 render(<MemoryRouter><PriceChart/></MemoryRouter>)
 await screen.findByText('No hay precios importados para este rango.')
 expect(screen.queryByTestId('chart')).toBeNull()
})
