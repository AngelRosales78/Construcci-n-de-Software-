import React from 'react'
import { it, expect, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MetricCards from './MetricCards'
import DataSources from './DataSources'
import FinancialHealthIndicator from './FinancialHealthIndicator'
import PriceChart from './PriceChart'
import api from '../../services/api'
import { countAvailableIndicators, hasIndicators } from '../../utils/financialPresentation'
vi.mock('../../services/api',()=>({default:{get:vi.fn()}}))
vi.mock('highcharts/highstock',()=>({default:{}}))
vi.mock('highcharts-react-official',()=>({default:({options})=><div data-testid="chart" data-points={options.series[0].data.length} data-crosshair={options.xAxis.crosshair} data-zoom={options.chart.zooming.type}/>}))
afterEach(()=>{cleanup();vi.resetAllMocks()})
it('conserva las fechas independientes de valoración bursátil y balance', () => {
 render(<MetricCards company={{per:15.9,roe:35.7,field_sources:{
  per:{as_of:'2026-10-26',source:'https://finance.yahoo.com/quote/GOOGL/',method:'Valoración reportada'},
  roe:{as_of:'2025-12-31',source:'https://data.sec.gov/example',method:'Beneficio sobre patrimonio promedio'}
 }}} />)
 expect(screen.getByText('Referencia · 2026-10-26')).toBeTruthy()
 expect(screen.getByText('Referencia · 2025-12-31')).toBeTruthy()
 expect(screen.queryByText('Consultar fuente del indicador')).toBeNull()
 render(<DataSources company={{field_sources:{per:{as_of:'2026-10-26',source:'https://finance.yahoo.com/quote/GOOGL/',method:'Valoración reportada'}}}} />)
 expect(screen.getByText('Valoración reportada')).toBeTruthy()
})
it('muestra cinco métricas, conserva cero y describe los indicadores',()=>{
 render(<MetricCards company={{per:0,roe:-2,market_cap:100,beta:null,dividend_yield:0}}/> )
 const meaning = screen.getByText(/Relaciona el beneficio con el patrimonio/)
 expect(meaning.closest('details')).toBeNull()
 expect(screen.getByText(/No representa tu ganancia como inversionista/)).toBeTruthy()
 expect(screen.getByText('Sin datos')).toBeTruthy()
 expect(screen.getAllByText('0.00%')).toHaveLength(1)
 expect(screen.getByText('-2.00%')).toBeTruthy()
})
it('muestra una clasificación explícita en color semántico',()=>{
 render(<FinancialHealthIndicator altmanZ={{zone:'grey',score:1.81}}/> )
 expect(screen.getByRole('heading',{name:'Zona intermedia'})).toBeTruthy()
 expect(screen.getByText('Según el modelo Altman-Z')).toBeTruthy()
 expect(screen.getByLabelText('Perfil financiero').className).toContain('orange')
})
it('distingue un cero real de indicadores vacíos, booleanos o no numéricos',()=>{
 const company={per:'',roe:0,market_cap:null,beta:'NaN',dividend_yield:false}
 render(<MetricCards company={company}/> )
 expect(countAvailableIndicators(company)).toBe(1)
 expect(hasIndicators(company)).toBe(true)
 expect(screen.getByText('0.00%')).toBeTruthy()
 expect(screen.getAllByText('Sin datos')).toHaveLength(4)
 expect(hasIndicators({...company,roe:'  '})).toBe(false)
})
it('no redondea un indicador pequeño disponible a un cero exacto',()=>{
 render(<MetricCards company={{dividend_yield:0.0012,roe:-0.0004}}/>)
 expect(screen.getByText('0.0012%')).toBeTruthy()
 expect(screen.getByText('-0.0004%')).toBeTruthy()
 expect(screen.queryByText('0.00%')).toBeNull()
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
 expect(screen.queryByRole('slider')).toBeNull()
})
it('permite consultar sesiones reales, incluido precio cero, sin otra petición',async()=>{
 api.get.mockResolvedValue({data:{history:[{date:'2025-01-02',price:0},{date:'2025-01-03',price:12.5}],sources:['https://query1.finance.yahoo.com/v8/finance/chart/DATA?range=5Y'],currencies:['USD'],is_demo:false}})
 render(<MemoryRouter initialEntries={['/company/DATA']}><Routes><Route path="/company/:ticker" element={<PriceChart/>}/></Routes></MemoryRouter>)
 const slider=await screen.findByRole('slider',{name:/Consultar un precio histórico/})
 expect(slider.getAttribute('aria-valuetext')).toBe('2025-01-03: 12.5 USD')
 expect(screen.getByRole('link',{name:'Yahoo Finance'}).getAttribute('href')).toBe('https://query1.finance.yahoo.com/v8/finance/chart/DATA?range=5Y')
 const calls=api.get.mock.calls.length
 fireEvent.change(slider,{target:{value:'0'}})
 expect(slider.getAttribute('aria-valuetext')).toBe('2025-01-02: 0 USD')
 expect(api.get.mock.calls.length).toBe(calls)
 fireEvent.click(screen.getByRole('button',{name:'6 meses',exact:true}))
 await waitFor(()=>expect(api.get).toHaveBeenLastCalledWith('/companies/DATA/history/?range=6M'))
 await waitFor(()=>expect(screen.getByRole('slider',{name:/Consultar un precio histórico/}).getAttribute('aria-valuetext')).toBe('2025-01-03: 12.5 USD'))
})
