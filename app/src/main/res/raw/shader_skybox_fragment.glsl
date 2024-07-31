precision mediump float;

uniform samplerCube u_TextureUnit;
uniform float u_SunElevation;
varying vec3 v_Position;

void main()
{
    float delta;
    float shadeAlpha;
    vec4 shadePixel;
    vec4 dayPixel = vec4(0.9, 0.9, 1.0, 0.0);
    vec4 goldenPixel = vec4(0.9, 0.6, 0.0, 1.0);
    vec4 civilPixel = vec4(0.0, 0.4, 0.7, 1.0);
    vec4 nauticalPixel = vec4(0.1, 0.0, 0.2, 1.0);
    vec4 nightPixel = vec4(0.0, 0.0, 0.0, 1.0);
    vec4 cubePixel = textureCube(u_TextureUnit, v_Position);

    if(u_SunElevation > 6.0)
    {
        shadePixel = dayPixel;
        shadeAlpha = 0.05;                                  //0.05
    }
    else if(u_SunElevation > -4.0)
    {
        delta = (6.0 - u_SunElevation) / 10.0;
        shadePixel = mix(dayPixel, goldenPixel, delta);
        shadeAlpha = 0.05 + (0.15 * delta);                 //0.05 - 0.2
    }
    else if(u_SunElevation > -6.0)
    {
        delta = (-4.0 - u_SunElevation) / 2.0;
        shadePixel = mix(goldenPixel, civilPixel, delta);
        shadeAlpha = 0.2 + (0.1 * delta);                   //0.2 - 0.3
    }
    else if(u_SunElevation > -12.0)
    {
        delta = (-6.0 - u_SunElevation) / 6.0;
        shadePixel = mix(civilPixel, nauticalPixel, delta);
        shadeAlpha = 0.3 + (0.3 * delta);                   //0.3 - 0.6
    }
    else
    {
        shadePixel = nightPixel;
        if(u_SunElevation > -20.0)
        {
            delta = (-12.0 - u_SunElevation) / 8.0;
            shadeAlpha = 0.6 + (0.25 * delta);              //0.6 - 0.85
        }
        else
        {
            shadeAlpha = 0.85;                              //0.85
        }
    }

    gl_FragColor = mix(cubePixel, shadePixel, shadeAlpha);
}