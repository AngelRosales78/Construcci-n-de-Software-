const paths = {
  overview: 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
  pillars: 'M3 12h4l3-7 4 14 3-7h4',
  simulation: 'M4 19V5m0 14h16M7 15l4-5 4 2 5-7',
  analysis: 'M5 5h14M5 10h10M5 15h14M5 20h8',
  technology: 'M7 7h10v10H7zM9 3v4m6-4v4m-6 10v4m6-4v4M3 9h4m-4 6h4m10-6h4m-4 6h4',
  healthcare: 'M9 3h6v6h6v6h-6v6H9v-6H3V9h6z',
  financials: 'M3 9l9-6 9 6H3zm2 3v6m5-6v6m4-6v6m5-6v6M3 21h18',
  industrials: 'M3 21V10l6 3V8l6 4V3h4l2 18H3zm3-4h1m4 0h1m5 0h1',
  energy: 'M13 2L4 14h7l-1 8 10-13h-7l1-7z',
  consumer_discretionary: 'M3 4h2l3 12h11l2-8H6m3 12h.01M18 20h.01',
}

export default function Icon({ name, size = 20 }) {
  return <svg className="ui-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false"><path d={paths[name] || paths.overview} /></svg>
}
