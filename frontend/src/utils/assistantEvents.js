export const ASSISTANT_EVENT='financial-assistant:open'
export function openAssistant(context={}) {
  window.dispatchEvent(new CustomEvent(ASSISTANT_EVENT,{detail:context}))
}
