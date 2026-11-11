import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import imagenBiblioteca from '../../assets/biblioteca-acceso.png';
import '../../styles/inicio-sesion.css';

export default function InicioSesion() {
  const [contrasenaVisible, cambiarContrasenaVisible] = useState(false);

  return (
    <main className="inicio-sesion">
      <span className="circulo-acceso circulo-acceso-superior" aria-hidden="true" />
      <span className="circulo-acceso circulo-acceso-inferior" aria-hidden="true" />

      <section className="tarjeta-acceso" aria-labelledby="titulo-inicio-sesion">
        <div className="panel-imagen-acceso">
          <img
            src={imagenBiblioteca}
            alt="Estudiantes y su tutora trabajando juntos en una biblioteca"
            className="imagen-biblioteca-acceso"
          />
        </div>

        <div className="panel-formulario-acceso">
          <p className="nombre-sistema-acceso">Control Académico</p>
          <header className="encabezado-acceso">
            <h1 id="titulo-inicio-sesion">Iniciar sesión</h1>
            <p>Ingresa tus datos para continuar</p>
          </header>

          <form className="formulario-acceso" noValidate onSubmit={(evento) => evento.preventDefault()}>
            <div className="campos-acceso">
              <div className="campo-acceso">
                <label htmlFor="correo-acceso">Correo</label>
                <input id="correo-acceso" name="correo" type="email" autoComplete="username" />
              </div>

              <div className="campo-acceso">
                <label htmlFor="contrasena-acceso">Contraseña</label>
                <div className="entrada-contrasena-acceso">
                  <input
                    id="contrasena-acceso"
                    name="contrasena"
                    type={contrasenaVisible ? 'text' : 'password'}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="visibilidad-contrasena-acceso"
                    onClick={() => cambiarContrasenaVisible(!contrasenaVisible)}
                    aria-label={contrasenaVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    aria-pressed={contrasenaVisible}
                    aria-controls="contrasena-acceso"
                  >
                    {contrasenaVisible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
                  </button>
                </div>
              </div>
            </div>

            <div className="opciones-acceso">
              <label className="recordar-acceso">
                <input type="checkbox" name="recordar" />
                <span>Recordar inicio de sesión</span>
              </label>
              <button type="button" className="enlace-acceso">¿Olvidaste tu contraseña?</button>
            </div>

            <button type="submit" className="boton-principal-acceso">Iniciar Sesión</button>

            <div className="separador-acceso"><span>o continúa con</span></div>

            <button type="button" className="boton-google-acceso">
              <svg className="icono-google-acceso" viewBox="0 0 48 48" aria-hidden="true">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z" />
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65Z" />
                <path fill="#FBBC05" d="M10.53 28.59A14.4 14.4 0 0 1 9.75 24c0-1.59.27-3.13.76-4.59l-7.98-6.19A23.85 23.85 0 0 0 0 24c0 3.87.93 7.52 2.56 10.78l7.97-6.19Z" />
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.91-5.8l-7.73-6c-2.15 1.45-4.92 2.3-8.18 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z" />
              </svg>
              Continuar con Google
            </button>

            <p className="registro-acceso">¿No tienes cuenta? <button type="button" className="enlace-acceso">Regístrate</button></p>
          </form>
        </div>
      </section>
    </main>
  );
}
