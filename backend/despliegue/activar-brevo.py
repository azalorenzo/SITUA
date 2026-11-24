import os
import pathlib
import smtplib
import ssl
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request


def leer(ruta):
    valores = {}
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        if linea.strip() and not linea.lstrip().startswith("#") and "=" in linea:
            clave, valor = linea.split("=", 1)
            valores[clave.strip()] = valor.strip()
    return valores


def guardar(ruta, contenido):
    descriptor, nombre = tempfile.mkstemp(dir=ruta.parent, prefix=".correo-")
    with os.fdopen(descriptor, "w", encoding="utf-8") as archivo:
        archivo.write(contenido)
    os.chmod(nombre, 0o600)
    os.replace(nombre, ruta)


def responde():
    import time
    for intento in range(40):
        try:
            urllib.request.urlopen("http://127.0.0.1:8095/api/autenticacion/sesion", timeout=2)
        except urllib.error.HTTPError as fallo:
            if fallo.code == 401:
                return True
        except (urllib.error.URLError, TimeoutError):
            pass
        time.sleep(1)
    return False


def activar():
    if os.geteuid() != 0:
        raise ValueError("Ejecuta este archivo con sudo.")
    ruta = pathlib.Path("/etc/situa/correo-brevo.env")
    destino = pathlib.Path("/etc/situa/.env")
    datos = leer(ruta)
    esperados = {
        "CORREO_HABILITADO": "true",
        "CORREO_SERVIDOR": "smtp-relay.brevo.com",
        "CORREO_PUERTO": "587",
        "CORREO_SSL": "false",
        "CORREO_AUTENTICACION": "true",
        "CORREO_STARTTLS": "true",
    }
    if any(datos.get(clave) != valor for clave, valor in esperados.items()):
        raise ValueError("Conserva el servidor Brevo, puerto 587, autenticación y STARTTLS del archivo preparado.")
    for clave in ("CORREO_USUARIO", "CORREO_CONTRASENA", "CORREO_REMITENTE"):
        if not datos.get(clave):
            raise ValueError("Completa " + clave + " en /etc/situa/correo-brevo.env.")
    if "@" not in datos["CORREO_REMITENTE"]:
        raise ValueError("Escribe el remitente verificado en Brevo.")
    if any(any(caracter.isspace() for caracter in valor) or any(caracter in valor for caracter in "\"'\\") for valor in datos.values()):
        raise ValueError("Escribe los valores sin espacios, comillas ni barras invertidas.")
    with smtplib.SMTP("smtp-relay.brevo.com", 587, timeout=10) as conexion:
        conexion.ehlo()
        conexion.starttls(context=ssl.create_default_context())
        conexion.ehlo()
        conexion.login(datos["CORREO_USUARIO"], datos["CORREO_CONTRASENA"])
    original = destino.read_text(encoding="utf-8")
    guardar(pathlib.Path("/etc/situa/.env.antes-brevo"), original)
    claves = set(esperados) | {"CORREO_USUARIO", "CORREO_CONTRASENA", "CORREO_REMITENTE"}
    lineas = [linea for linea in original.splitlines() if linea.split("=", 1)[0].strip() not in claves]
    lineas.extend(clave + "=" + datos[clave] for clave in sorted(claves))
    guardar(destino, "\n".join(lineas) + "\n")
    try:
        subprocess.run(["systemctl", "restart", "situa-backend"], check=True)
        if not responde():
            raise RuntimeError("SITUA no respondió después del reinicio.")
    except Exception:
        guardar(destino, original)
        subprocess.run(["systemctl", "restart", "situa-backend"], check=False)
        raise RuntimeError("Se restauró la configuración anterior del correo de SITUA.") from None
    print("Brevo configurado en SITUA. Comprueba ahora la recepción de un código; la autenticación SMTP no acredita la entrega.")


if __name__ == "__main__":
    try:
        activar()
    except (ValueError, RuntimeError) as fallo:
        print(str(fallo), file=sys.stderr)
        sys.exit(1)
    except Exception:
        print("No se pudo completar la conexión segura con Brevo. Revisa las credenciales SMTP y la activación de tu cuenta. No se muestran datos privados.", file=sys.stderr)
        sys.exit(1)
