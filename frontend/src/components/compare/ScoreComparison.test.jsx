import React from 'react'
import { afterEach, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import ScoreComparison from './ScoreComparison'

afterEach(cleanup)
it('preserva la precisión presentada y dibuja solamente valores comparables', () => {
  const score = {status:'available', value:65.123, display_value:'65,12'}
  const {container, rerender} = render(<ScoreComparison score={score} side="a" comparable />)
  expect(screen.getByText('65,12 / 100')).toBeTruthy()
  expect(container.querySelector('.comparison-score-track > span').style.width).toBe('65.123%')
  rerender(<ScoreComparison score={score} side="a" comparable={false} />)
  expect(screen.getByText('65,12 / 100')).toBeTruthy()
  expect(container.querySelector('.comparison-score-track')).toBeNull()
})
it('conserva un cero real sin convertir un dato ausente o fuera de escala en una barra', () => {
  const {container, rerender} = render(<ScoreComparison score={{status:'available',value:0,display_value:'0,00'}} side="b" comparable />)
  expect(container.querySelector('.comparison-score-track > span').style.width).toBe('0%')
  for (const value of [null, undefined, NaN, 101, -1, '', '  ', false]) {
    rerender(<ScoreComparison score={{status:'available',value,display_value:'Referencia'}} side="b" comparable />)
    expect(container.querySelector('.comparison-score-track')).toBeNull()
  }
  rerender(<ScoreComparison score={{status:'unavailable',value:null}} side="b" comparable />)
  expect(screen.getByText('No disponible')).toBeTruthy()
})
