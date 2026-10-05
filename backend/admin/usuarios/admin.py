from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import Usuario, Rol


@admin.register(Rol)
class RolAdmin(admin.ModelAdmin):
    list_display = ('id', 'nombre')


@admin.register(Usuario)
class UsuarioAdmin(UserAdmin):
    fieldsets = UserAdmin.fieldsets + (('Rol', {'fields': ('rol',)}),)
    add_fieldsets = UserAdmin.add_fieldsets + (('Rol', {'fields': ('email', 'rol')}),)
    list_display = ('username', 'email', 'rol', 'is_active')