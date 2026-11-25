import api from '../services/api'

function endpoint(context) {
  return context.b ? '/compare/assistant/' : `/companies/${encodeURIComponent(context.a)}/assistant/`
}
function params(context) {
  return {fiscal_year:context.year || undefined,...(context.b ? {a:context.a,b:context.b} : {})}
}
export const assistantRepository = {
  read:(context,signal)=>api.get(endpoint(context),{params:params(context),signal}).then(response=>response.data),
  send:(context,payload,signal)=>api.post(endpoint(context),{...params(context),...payload},{signal,timeout:100000}).then(response=>response.data),
}
