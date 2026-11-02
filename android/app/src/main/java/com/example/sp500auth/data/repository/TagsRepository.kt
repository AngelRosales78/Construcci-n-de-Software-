package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

data class TagItem(val id:Long?=null,val name:String?=null,val color:String?=null)
data class TagAssignment(val favorite_id:Long?=null,val tag_ids:List<Long?>?=null)
data class TagsSnapshot(val schema_version:String?=null,val owner_id:String?=null,val version:String?=null,val tags:List<TagItem?>?=null,val assignments:List<TagAssignment?>?=null)
data class TagEdit(val name:String,val color:String,val version:String)
data class TagSelection(val tag_ids:List<Long>,val version:String)
fun normalizedTag(name:String)=Normalizer.normalize(name.trim().replace(Regex("\\s+")," "),Normalizer.Form.NFC).lowercase(Locale.ROOT)
fun validTagName(name:String)=name.trim().length in 1..40 && name.none {it.isISOControl()}
fun validTags(snapshot:TagsSnapshot,user:String):Boolean=runCatching {
    val tags=snapshot.tags ?: return false;val assignments=snapshot.assignments ?: return false
    val ids=tags.map{it?.id}.toSet()
    snapshot.schema_version=="tags-v1" && snapshot.owner_id==user && !snapshot.version.isNullOrBlank() && tags.size<=100 &&
        ids.size==tags.size && tags.all{it!=null && (it.id ?: 0)>0 && validTagName(it.name.orEmpty()) && it.color?.matches(Regex("#[0-9a-fA-F]{6}"))==true} &&
        tags.map{normalizedTag(it!!.name!!)}.distinct().size==tags.size && assignments.size<=10000 && assignments.map{it?.favorite_id}.distinct().size==assignments.size &&
        assignments.all{it!=null && (it.favorite_id ?: 0)>0 && it.tag_ids!=null && it.tag_ids.distinct().size==it.tag_ids.size && it.tag_ids.all {id->id in ids}}
}.getOrDefault(false)
interface TagsApi {
    @GET("tags/") suspend fun get(@Header("Authorization") token:String,@Tag account:AccountBinding):Response<TagsSnapshot>
    @POST("tags/") suspend fun create(@Body value:TagEdit,@Header("Authorization") token:String,@Tag account:AccountBinding):Response<TagsSnapshot>
    @PATCH("tags/{id}/") suspend fun edit(@Path("id")id:Long,@Body value:TagEdit,@Header("Authorization") token:String,@Tag account:AccountBinding):Response<TagsSnapshot>
    @DELETE("tags/{id}/") suspend fun delete(@Path("id")id:Long,@Query("version") version:String,@Header("Authorization") token:String,@Tag account:AccountBinding):Response<TagsSnapshot>
    @PUT("favorites/{id}/tags/") suspend fun assign(@Path("id")id:Long,@Body value:TagSelection,@Header("Authorization") token:String,@Tag account:AccountBinding):Response<TagsSnapshot>
}
interface TagsSource {
    fun owner():String?;suspend fun load():Resource<TagsSnapshot>
    suspend fun edit(id:Long?,name:String,color:String,version:String):Resource<TagsSnapshot>
    suspend fun delete(id:Long,version:String):Resource<TagsSnapshot>
    suspend fun assign(id:Long,tags:List<Long>,version:String):Resource<TagsSnapshot>
}
class TagsRepository @Inject constructor(private val remote:ScopedRemote):TagsSource {
    private val api=remote.retrofit.create(TagsApi::class.java)
    override fun owner()=remote.owner()
    override suspend fun load()=remote.call({data:TagsSnapshot,user->validTags(data,user)}){token,account->api.get(token,account)}
    override suspend fun edit(id:Long?,name:String,color:String,version:String):Resource<TagsSnapshot>{
        if((id!=null && id<=0) || !validTagName(name) || !color.matches(Regex("#[0-9a-fA-F]{6}")) || version.isBlank())return Resource.Error("Nombre, color o versión de etiqueta no válidos.")
        return remote.call({data:TagsSnapshot,user->validTags(data,user)}){token,account->if(id==null)api.create(TagEdit(name.trim(),color,version),token,account)else api.edit(id,TagEdit(name.trim(),color,version),token,account)}
    }
    override suspend fun delete(id:Long,version:String):Resource<TagsSnapshot>{
        if(id<=0 || version.isBlank())return Resource.Error("Etiqueta no válida.")
        return remote.call({data:TagsSnapshot,user->validTags(data,user) && data.tags.orEmpty().none{it?.id==id}}){token,account->api.delete(id,version,token,account)}
    }
    override suspend fun assign(id:Long,tags:List<Long>,version:String):Resource<TagsSnapshot>{
        if(id<=0 || tags.any{it<=0} || tags.distinct().size!=tags.size || version.isBlank())return Resource.Error("Asignación no válida.")
        return remote.call({data:TagsSnapshot,user->validTags(data,user) && data.assignments?.singleOrNull{it?.favorite_id==id}?.tag_ids?.toSet()==tags.toSet()}){token,account->api.assign(id,TagSelection(tags,version),token,account)}
    }
}
