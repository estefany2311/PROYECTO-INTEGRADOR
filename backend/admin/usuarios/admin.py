from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import Permiso, Rol, RolPermiso, Usuario


class RolPermisoInline(admin.TabularInline):
    model = RolPermiso
    extra = 0
    autocomplete_fields = ('permiso',)


@admin.register(Rol)
class RolAdmin(admin.ModelAdmin):
    list_display = ('id', 'nombre')
    inlines = [RolPermisoInline]


@admin.register(Permiso)
class PermisoAdmin(admin.ModelAdmin):
    list_display = ('codigo', 'modulo', 'accion')
    list_filter = ('modulo',)
    search_fields = ('codigo', 'modulo')


@admin.register(Usuario)
class UsuarioAdmin(UserAdmin):
    fieldsets = UserAdmin.fieldsets + (('Rol', {'fields': ('rol',)}),)
    add_fieldsets = UserAdmin.add_fieldsets + (('Rol', {'fields': ('email', 'rol')}),)
    list_display = ('username', 'email', 'rol', 'is_active')