import Icon from './Icon'
import {openAssistant} from '../../utils/assistantEvents'

export default function AskAI({ticker,year,question,mode='question',children='Explícame con IA',className=''}) {
  return <button type="button" className={`ask-ai ${className}`} aria-haspopup="dialog" onClick={()=>openAssistant({ticker,year,question,mode})}><Icon name="chat" size={18}/>{children}<span className="ai-label" aria-hidden="true">IA</span></button>
}
