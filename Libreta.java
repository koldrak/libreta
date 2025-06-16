
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.InputMismatchException;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.jsoup.Jsoup;
import javax.swing.text.DefaultEditorKit;

public class Libreta {
	static DefaultListModel <Nota> Notas = new DefaultListModel <>();
	static ArrayList<Nota> todasLasNotas = new ArrayList<>();

	public static void main(String[] args) {

		todasLasNotas = Nota.cargarNotas();
		todasLasNotas.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
		for (Nota n : todasLasNotas) {
			Notas.addElement(n);
		}

		JFrame mainmenu = new JFrame("Libreta de apuntes");
		mainmenu.setSize(300, 400);
		mainmenu.setAlwaysOnTop(true);
		mainmenu.setLayout(new BorderLayout());
		JPanel Superior = new JPanel();
		JPanel Centro = new JPanel(new BorderLayout());
		JPanel Inferior = new JPanel(new BorderLayout());

		//Funcionamaniento de botones
		JButton boton1 = new JButton("Agregar Nota");
		JButton boton2 = new JButton("Exportar Word");


		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == boton1) {
					new Formulario ();
				}
			}
		};
		ActionListener listener2 = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == boton2) {
				    exportarNotasAWord();
				}
			}
		};
		//JList con nombres de notas
		JList <Nota> lista = new JList<>(Notas);
		JScrollPane scrollLista = new JScrollPane(lista);
		scrollLista.setPreferredSize(new Dimension(250, 200)); // Opcional: ajustar tamaño visible

		lista.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = lista.locationToIndex(e.getPoint()); // Índice del ítem bajo el mouse

				if (index >= 0) {
					lista.setSelectedIndex(index); // Resalta la nota
					Nota seleccionada = lista.getModel().getElementAt(index);

					// Acción para clic derecho
					if (SwingUtilities.isRightMouseButton(e)) {
						int confirmacion = JOptionPane.showConfirmDialog(null,
								"¿Eliminar la nota \"" + seleccionada.titulo + "\"?",
								"Confirmar eliminación", JOptionPane.YES_NO_OPTION);

						if (confirmacion == JOptionPane.YES_OPTION) {
							Libreta.Notas.removeElementAt(index);
							ArrayList<Nota> respaldo = Collections.list(Notas.elements());
							Nota.guardarNotas(respaldo);
						}

						// Acción para clic izquierdo
					} else if (SwingUtilities.isLeftMouseButton(e)) {
						new VentanaNota(seleccionada);
					}
				}
			}
		});


		// Campo de búsqueda
		JTextField buscador = new JTextField(25);
		buscador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));


		// Filtro dinámico
		buscador.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				filtrarNotas();
			}

			private void filtrarNotas() {
				String texto = buscador.getText().toLowerCase();
				Notas.clear();
				for (Nota n : todasLasNotas) {
					String[] palabrasClave = texto.split("\\s+");
					String titulo = n.titulo.toLowerCase();
					boolean coincide = true;

					for (String palabra : palabrasClave) {
						if (!titulo.contains(palabra)) {
							coincide = false;
							break;
						}
					}

					if (coincide) {
						Notas.addElement(n);
					}

				}
			}
		});

		//Orden de ventanas

		mainmenu.add(Superior, BorderLayout.NORTH);
		boton1.addActionListener(listener);
		boton2.addActionListener(listener2);   
		Superior.add(boton1);
		Superior.add(boton2);

		mainmenu.add(Centro, BorderLayout.CENTER);
		Centro.add(scrollLista);     

		mainmenu.add(Inferior, BorderLayout.SOUTH);
		Inferior.add(buscador);

		//cierre de ventana
		mainmenu.setVisible(true);
		mainmenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
    public static void exportarNotasAWord() {
        try {
            XWPFDocument doc = new XWPFDocument();

	        for (Nota nota : todasLasNotas) {
	            // Título de la nota
	            XWPFParagraph titulo = doc.createParagraph();
	            XWPFRun runTitulo = titulo.createRun();
	            runTitulo.setText(nota.titulo);
	            runTitulo.setBold(true);
	            runTitulo.setFontSize(16);

	            // Analizar contenido HTML
	            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(nota.contenidoHTML);
	            for (org.jsoup.nodes.Element elem : htmlDoc.select("*")) {
	                if (elem.tagName().equals("img")) {
	                    String src = elem.attr("src").trim();
	                    int ancho = 200;
	                    int alto = 200;

	                    try {
	                        ancho = Integer.parseInt(elem.attr("width"));
	                        alto = Integer.parseInt(elem.attr("height"));
	                    } catch (NumberFormatException e) {
	                        // se usan valores por defecto
	                    }

	                    File imageFile;
	                    if (src.contains("/") || src.contains("\\")) {
	                        imageFile = new File(src);
	                    } else {
	                        imageFile = new File("imagenes", src);
	                    }

	                    if (imageFile.exists()) {
	                        try (InputStream pic = new FileInputStream(imageFile)) {
	                            XWPFParagraph p = doc.createParagraph();
	                            XWPFRun r = p.createRun();
	                            r.addPicture(pic,
	                                    XWPFDocument.PICTURE_TYPE_PNG,
	                                    src,
	                                    Units.toEMU(ancho),
	                                    Units.toEMU(alto));
	                            System.out.println("Imagen encontrada: " + imageFile.getAbsolutePath());
	                        }
	                    } else {
	                        XWPFParagraph p = doc.createParagraph();
	                        XWPFRun r = p.createRun();
	                        r.setItalic(true);
	                        r.setText("[Imagen no encontrada: " + src + "]");
	                        System.out.println("NO SE ENCONTRÓ: " + imageFile.getAbsolutePath());
	                    }
	                } else {
	                    String texto = elem.ownText();
	                    if (!texto.isEmpty()) {
	                        XWPFParagraph p = doc.createParagraph();
	                        XWPFRun r = p.createRun();
	                        // Color de fondo si hay
	                        String bgColor = elem.attr("style");
	                        if (bgColor.contains("background-color")) {
	                            // ejemplo: style="background-color:#87CEEB"
	                            String colorHex = bgColor.split("background-color:")[1].split(";")[0].trim();
	                            r.setTextHighlightColor(colorHex.replace("#", ""));
	                        }
                            r.setText(texto);
                        }
                    }
                }

                doc.createParagraph(); // salto entre notas
            }

	        try (FileOutputStream out = new FileOutputStream("NotasExportadas.docx")) {
	            doc.write(out);
	        }

	        JOptionPane.showMessageDialog(null, "Notas exportadas correctamente a NotasExportadas.docx");
	    } catch (Exception e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(null, "Error al exportar: " + e.getMessage());
	    }
    }

    /**
     * Reemplaza los saltos de línea ("\n") del documento por etiquetas
     * HTML <br> para que se conserven al guardar la nota.
     */
    static void reemplazarSaltosDeLinea(HTMLDocument doc, HTMLEditorKit kit) {
        try {
            String texto = doc.getText(0, doc.getLength());
            int pos = 0;
            while ((pos = texto.indexOf('\n', pos)) != -1) {
                doc.remove(pos, 1);
                kit.insertHTML(doc, pos, "<br>", 0, 0, HTML.Tag.BR);
                texto = doc.getText(0, doc.getLength());
                pos += 4; // avanza tras la etiqueta insertada
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

class Nota implements Serializable {
	String titulo;
	String contenidoHTML;
	
	public Nota (String ti,String con) {
		titulo = ti;
		contenidoHTML = con;
		Libreta.Notas.addElement(this);
	}
	
	public String toString() {
		return titulo;
	}
	
	public static void guardarNotas(ArrayList<Nota> lista) {
	    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("notas.dat"))) {
	        out.writeObject(lista);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static ArrayList<Nota> cargarNotas() {
	    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("notas.dat"))) {
	        return (ArrayList<Nota>) in.readObject();
	    } catch (Exception e) {
	        return new ArrayList<>(); // Si no existe o da error, inicia vacío
	    }
	}

}

class Formulario extends JFrame {
	StringWriter writer = new StringWriter();
	HTMLEditorKit kit = new HTMLEditorKit();
	public String ultimoColorHTML = null;
	ArrayList<ImageIcon> imagenesPegadas = new ArrayList<>();

	private BufferedImage toBufferedImage(Image img) {
	    if (img instanceof BufferedImage) {
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(
	        img.getWidth(null), img.getHeight(null),
	        BufferedImage.TYPE_INT_ARGB
	    );

	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    return bimage;
	}

	private String limpiarTitulo(String titulo) {
	    return titulo.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}

    public Formulario() {
        this.setTitle("FORMULARIO");
        this.setSize(500, 400);
        this.setAlwaysOnTop(true);
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS)); 
        JPanel Inferior = new JPanel(); // usa FlowLayout por defecto
        JPanel Superior = new JPanel(); // usa FlowLayout por defecto
        JPanel Central = new JPanel(new BorderLayout());
        
        //Campo para ingresar titulo de la nota 
        JLabel etiquetatit = new JLabel("Titulo del texto");
        JTextField campotit = new JTextField(30); //crear text field
        campotit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

        
      //Campo para ingresar texto de la nota 
        JLabel etiquetatxt = new JLabel("ingresa el codigo");
        JTextPane campotxt = new JTextPane() {
        	@Override
        	public void paste() {
        	    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        	    if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        	        try {
        	            Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
        	            ImageIcon icon = new ImageIcon(img);

        	            // Insertamos como icono visual
                        this.insertIcon(icon);
                        // Evita que el icono se replique al escribir
                        StyledEditorKit sek = (StyledEditorKit) this.getEditorKit();
                        sek.getInputAttributes().removeAttribute(StyleConstants.IconAttribute);
                        
        	            // Guardamos temporalmente la imagen en una lista
        	            if (imagenesPegadas == null) {
        	                imagenesPegadas = new ArrayList<>();
        	            }
        	            imagenesPegadas.add(icon);

        	        } catch (Exception ex) {
        	            ex.printStackTrace();
        	        }
        	    } else {
        	        super.paste();
        	    }
        	}

        };
        campotxt.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int pos = campotxt.viewToModel2D(e.getPoint()); // <- usa viewToModel2D para precisión
                    StyledDocument doc = campotxt.getStyledDocument();
                    Element elem = doc.getCharacterElement(pos);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;

                        String anchoStr = JOptionPane.showInputDialog(null, "Ancho (px):", imgIcon.getIconWidth());
                        String altoStr = JOptionPane.showInputDialog(null, "Alto (px):", imgIcon.getIconHeight());

                        if (anchoStr != null && altoStr != null) {
                            try {
                                int ancho = Integer.parseInt(anchoStr);
                                int alto = Integer.parseInt(altoStr);
                                Image nueva = imgIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                                ImageIcon redimensionado = new ImageIcon(nueva);

                                SimpleAttributeSet sas = new SimpleAttributeSet();
                                StyleConstants.setIcon(sas, redimensionado);

                                // Reemplaza el ícono anterior
                                doc.remove(elem.getStartOffset(), 1);
                                doc.insertString(elem.getStartOffset(), " ", sas);
                            } catch (NumberFormatException | BadLocationException ex) {
                                JOptionPane.showMessageDialog(null, "Entrada inválida.");
                            }
                        }
                    }
                }
            }
        });


        HTMLEditorKit kit = new HTMLEditorKit(); // 🆕
        campotxt.setEditorKit(kit);              // 🆕
        campotxt.setContentType("text/html");    // 🆕
        
        campotxt.setPreferredSize(new Dimension(350, 200));
        JScrollPane scroll = new JScrollPane(campotxt); // Para permitir desplazamiento si el texto es largo
        campotxt.setFont(new Font("Arial", Font.BOLD,16));
        
        // Ctrl + N: aplicar negrita
        campotxt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "negrita" );
        campotxt.getActionMap().put( "negrita" , new StyledEditorKit .BoldAction());
        
        // Enter: iniciar nueva línea sin mantener negrita
        campotxt.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newline-no-bold");
        campotxt.getActionMap().put("newline-no-bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DefaultEditorKit.InsertBreakAction().actionPerformed(e);
                StyledEditorKit sek = (StyledEditorKit) campotxt.getEditorKit();
                sek.getInputAttributes().removeAttribute(StyleConstants.Bold);
            }
        });
        
     // Shift + Tab: aplicar color
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "aplicarUltimoColor");
        campotxt.getActionMap().put("aplicarUltimoColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int start = campotxt.getSelectionStart();
                    int end = campotxt.getSelectionEnd();

                    if (start == end) {
                        // No hay selección → buscar palabra a la izquierda
                        int pos = start - 1;
                        String texto = campotxt.getText(0, campotxt.getDocument().getLength());

                        while (pos >= 0 && Character.isWhitespace(texto.charAt(pos))) pos--;
                        int fin = pos + 1;
                        while (pos >= 0 && Character.isLetterOrDigit(texto.charAt(pos))) pos--;
                        start = pos + 1;
                        end = fin;
                    }

                    if (start >= end || ultimoColorHTML == null) return;

                    String original = campotxt.getDocument().getText(start, end - start);
                    String nuevoTexto;

                    if (ultimoColorHTML.equals("#FFFFFF")) {
                        nuevoTexto = "<span style=\"background-color:#FFFFFF; color:#000000\">" + original + "</span>";
                    } else {
                        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
                    }

                    javax.swing.text.Document doc = campotxt.getDocument();
                    doc.remove(start, end - start);
                    kit.insertHTML((HTMLDocument) doc, start, nuevoTexto, 0, 0, HTML.Tag.SPAN);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Alt + C: cambiar al siguiente color
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("alt C"), "siguienteColor");
        campotxt.getActionMap().put("siguienteColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] colores = {"#808080", "#90EE90", "#CC9900", "#87CEEB"};
                int index = -1;
                for (int i = 0; i < colores.length; i++) {
                    if (colores[i].equalsIgnoreCase(ultimoColorHTML)) {
                        index = i;
                        break;
                    }
                }
                int siguiente = (index + 1) % colores.length;
                ultimoColorHTML = colores[siguiente];
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });

     // Alt + X: cambiar al color "Sin destacar"
        campotxt.getInputMap().put(KeyStroke.getKeyStroke("alt X"), "colorSinDestacar");
        campotxt.getActionMap().put("colorSinDestacar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ultimoColorHTML = "#FFFFFF";
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });


        
        //boton de guardar nota
        JButton botcrearnota = new JButton("Crear nueva nota");
        
        botcrearnota.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String valortit = campotit.getText(); // obtener valor de text field
               	String valortxt = campotxt.getText(); // obtener valor de text field
               	
                campotxt.setContentType("text/html");
                campotxt.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            	try {
            	    kit.write(writer, campotxt.getDocument(), 0, campotxt.getDocument().getLength());
            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}
                String html = writer.toString();
                try {
                    StyledDocument doc = campotxt.getStyledDocument();
                    int imgIndex = 0;
                    for (int i = 0; i < doc.getLength(); i++) {
                        Element elem = doc.getCharacterElement(i);
                        AttributeSet attr = elem.getAttributes();
                        Icon icon = StyleConstants.getIcon(attr);

                        if (icon instanceof ImageIcon) {
                            ImageIcon imgIcon = (ImageIcon) icon;
                            String nombreArchivo = limpiarTitulo(valortit) + "_img" + imgIndex + ".png";


                            // Guarda la imagen en disco
                            File imgDir = new File("imagenes");
                            if (!imgDir.exists()) imgDir.mkdir();
                            File output = new File(imgDir, nombreArchivo);
                            ImageIO.write(toBufferedImage(imgIcon.getImage()), "png", output);

                            // Reemplaza el icono embebido por un <img src="imagenes/imgX.png">
                            SimpleAttributeSet sas = new SimpleAttributeSet();
                            StyleConstants.setIcon(sas, null);  // Borra icono visual
                            doc.remove(elem.getStartOffset(), 1);
                            kit.insertHTML((HTMLDocument) doc, elem.getStartOffset(),
                            	    "<img src='" + nombreArchivo + "' width='" + imgIcon.getIconWidth()
                            	    + "' height='" + imgIcon.getIconHeight() + "'>", 0, 0, HTML.Tag.IMG);

                            imgIndex++;
                        }
                    }
                    // Reemplaza los saltos de línea por <br> antes de generar el HTML
                    Libreta.reemplazarSaltosDeLinea((HTMLDocument) doc, kit);

                    // Ahora sí, escribir HTML ya procesado
                    writer = new StringWriter(); // reinicia
                    kit.write(writer, doc, 0, doc.getLength());
                    String htmlFinal = writer.toString();

                    Nota notax = new Nota(valortit, htmlFinal);

                    // Reordenar y guardar
                    ArrayList<Nota> respaldo = Collections.list(Libreta.Notas.elements());
                    respaldo.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
                    Libreta.Notas.clear();
                    for (Nota n : respaldo) Libreta.Notas.addElement(n);
                    Nota.guardarNotas(respaldo);

                    Formulario.this.dispose();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                
             // Reordenar notas antes de guardar
             ArrayList<Nota> respaldo = Collections.list(Libreta.Notas.elements());
             respaldo.sort((a, b) -> a.titulo.compareToIgnoreCase(b.titulo));
             Libreta.Notas.clear(); // Vaciar modelo para reinsertar ordenado
             for (Nota n : respaldo) {
                 Libreta.Notas.addElement(n);
                 
             }
             Nota.guardarNotas(respaldo);

                
                Formulario.this.dispose(); 
            }
        });
        

        
        //Jlist seleccion de color 
        String[] colores = {"Gris", "Verde", "Amarillo", "Celeste","Sin destacar"};
        JList<String> listacolor = new JList<>(colores);

        listacolor.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            	try {
            	    int start = campotxt.getSelectionStart();
            	    int end = campotxt.getSelectionEnd();
            	    if (start == end) return;
            	    String original = campotxt.getSelectedText();
            	    String nuevoTexto;
            	    String seleccionado = listacolor.getSelectedValue();


            	    switch (seleccionado) {
            	    case "Gris":
            	        ultimoColorHTML = "#808080";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Verde":
            	        ultimoColorHTML = "#90EE90";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Amarillo":
            	        ultimoColorHTML = "#CC9900";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Celeste":
            	        ultimoColorHTML = "#87CEEB";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Sin destacar":
            	        ultimoColorHTML = "#FFFFFF";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "; color:#000000\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    default:
            	        nuevoTexto = original;
            	}

          
            	    javax.swing.text.Document doc = campotxt.getDocument();
            	    doc.remove(start, end - start);
            	    kit.insertHTML((HTMLDocument) doc, start, nuevoTexto, 0, 0, HTML.Tag.SPAN);


            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}

            	listacolor.setVisible(false);



            }
        });
        
        
        listacolor.setVisible(false);
        
        
        //Boton para cambiar estilo
        JButton destacar = new JButton("Destacar");
        destacar.addActionListener(e -> {
        	if (listacolor.isVisible()==false) {
            	listacolor.setVisible(true);  
        	} else {
        		listacolor.setVisible(false);
        	}
        });
        
        //Spinner
        Integer[] datos = {8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100 };

        JSpinner lista = new JSpinner(new SpinnerListModel(datos));
        lista.setPreferredSize(new Dimension(100, 30));
        lista.setVisible(true);
        lista.setValue(16);

        lista.addChangeListener(e -> {
            int nuevoTamaño = (int) lista.getValue();
            
            javax.swing.text.html.StyleSheet estilo = kit.getStyleSheet();

            // Elimina reglas anteriores y aplica nuevo tamaño
            estilo.addRule("body { font-size: " + nuevoTamaño + "pt; }");

            // Recarga el contenido actual para aplicar el nuevo estilo
            String htmlActual = campotxt.getText();
            campotxt.setText("");           // limpia para forzar recarga
            campotxt.setText(htmlActual);   // recarga con nueva hoja de estilo
        });

        
        this.add(Superior, BorderLayout.NORTH);
        Superior.add(etiquetatit);
        Superior.add(campotit);
        
        this.add(Central, BorderLayout.CENTER);
        Central.add(etiquetatxt);
        Central.add(scroll);
        
        this.add(Inferior, BorderLayout.SOUTH);
        Inferior.add(destacar);
        Inferior.add(listacolor);
        Inferior.add(lista);
        Inferior.add (botcrearnota);
  

       
        //Cierre
        
        Formulario.this.dispose(); 
        this.setVisible(true);
    }
}

class VentanaNota extends JFrame {
	public String ultimoColorHTML = null;
	StringWriter writer = new StringWriter();
	HTMLEditorKit kit = new HTMLEditorKit();
	
	private BufferedImage toBufferedImage(Image img) {
	    if (img instanceof BufferedImage) {
	        return (BufferedImage) img;
	    }

	    BufferedImage bimage = new BufferedImage(
	        img.getWidth(null), img.getHeight(null),
	        BufferedImage.TYPE_INT_ARGB
	    );

	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    return bimage;
	}
	private String limpiarTitulo(String titulo) {
	    return titulo.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}

	public VentanaNota(Nota not) {
        this.setTitle(not.titulo);
        this.setAlwaysOnTop(true);
        this.setSize(400, 400);
        this.setLayout(new BorderLayout());
        JPanel panelInferior = new JPanel(); // usa FlowLayout por defecto
        this.add(panelInferior, BorderLayout.SOUTH);
        
        //Area de texto
        JTextPane areatexto = new JTextPane() {
            @Override
            public void paste() {
                Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        ImageIcon icon = new ImageIcon(img);
                        this.insertIcon(icon);
                        
                        // Evita que el icono se replique al escribir
                        StyledEditorKit sek = (StyledEditorKit) this.getEditorKit();
                        sek.getInputAttributes().removeAttribute(StyleConstants.IconAttribute);
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    super.paste();
                }
            }
        };
        areatexto.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int pos = areatexto.viewToModel2D(e.getPoint()); // <- usa viewToModel2D para precisión
                    StyledDocument doc = areatexto.getStyledDocument();
                    Element elem = doc.getCharacterElement(pos);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;

                        String anchoStr = JOptionPane.showInputDialog(null, "Ancho (px):", imgIcon.getIconWidth());
                        String altoStr = JOptionPane.showInputDialog(null, "Alto (px):", imgIcon.getIconHeight());

                        if (anchoStr != null && altoStr != null) {
                            try {
                                int ancho = Integer.parseInt(anchoStr);
                                int alto = Integer.parseInt(altoStr);
                                Image nueva = imgIcon.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                                ImageIcon redimensionado = new ImageIcon(nueva);

                                SimpleAttributeSet sas = new SimpleAttributeSet();
                                StyleConstants.setIcon(sas, redimensionado);

                                // Reemplaza el ícono anterior
                                doc.remove(elem.getStartOffset(), 1);
                                doc.insertString(elem.getStartOffset(), " ", sas);
                            } catch (NumberFormatException | BadLocationException ex) {
                                JOptionPane.showMessageDialog(null, "Entrada inválida.");
                            }
                        }
                    }
                }
            }
        });


        
        areatexto.setEditorKit(kit);
        areatexto.setContentType("text/html");

        HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
        try {
            // Establece la base URL para que las etiquetas <img src="imagenes/archivo.png"> funcionen
            java.net.URL baseURL = new java.io.File("imagenes").toURI().toURL();
            doc.setBase(baseURL);

            kit.read(new java.io.StringReader(not.contenidoHTML), doc, 0);
            areatexto.setDocument(doc);
            
            try {
                HTMLDocument htmlDoc = (HTMLDocument) areatexto.getDocument();
                ElementIterator iterator = new ElementIterator(htmlDoc);
                Element elem;

                while ((elem = iterator.next()) != null) {
                    AttributeSet attrs = elem.getAttributes();
                    Object name = attrs.getAttribute(StyleConstants.NameAttribute);

                    if (name == HTML.Tag.IMG) {
                        String src = (String) attrs.getAttribute(HTML.Attribute.SRC);
                        String widthStr = (String) attrs.getAttribute(HTML.Attribute.WIDTH);
                        String heightStr = (String) attrs.getAttribute(HTML.Attribute.HEIGHT);

                        int ancho = (widthStr != null) ? Integer.parseInt(widthStr) : 200;
                        int alto = (heightStr != null) ? Integer.parseInt(heightStr) : 200;

                        File imgFile = new File("imagenes", src);
                        if (!imgFile.exists()) continue;

                        ImageIcon icon = new ImageIcon(new ImageIcon(imgFile.getAbsolutePath())
                                .getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));

                        SimpleAttributeSet sas = new SimpleAttributeSet();
                        StyleConstants.setIcon(sas, icon);

                        htmlDoc.remove(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
                        htmlDoc.insertString(elem.getStartOffset(), " ", sas);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        areatexto.setEditable(true);
        JScrollPane scrollarea = new JScrollPane(areatexto); // Para permitir desplazamiento si el texto es largo
        areatexto.setFont(new Font("Arial", Font.BOLD,16));
     // Ctrl + N: aplicar negrita
        areatexto.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "negrita" );
        areatexto.getActionMap().put( "negrita" , new StyledEditorKit .BoldAction());
        // Enter: iniciar nueva línea sin mantener negrita
        areatexto.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newline-no-bold");
        areatexto.getActionMap().put("newline-no-bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DefaultEditorKit.InsertBreakAction().actionPerformed(e);
                StyledEditorKit sek = (StyledEditorKit) areatexto.getEditorKit();
                sek.getInputAttributes().removeAttribute(StyleConstants.Bold);
            }
        });
        
        areatexto.setEditable(true);
        this.add(scrollarea);
        
        
     // Shift + Tab: aplicar color
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "aplicarUltimoColor");
        areatexto.getActionMap().put("aplicarUltimoColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int start = areatexto.getSelectionStart();
                    int end = areatexto.getSelectionEnd();

                    if (start == end) {
                        // No hay selección → buscar palabra a la izquierda
                        int pos = start - 1;
                        String texto = areatexto.getText(0, areatexto.getDocument().getLength());

                        while (pos >= 0 && Character.isWhitespace(texto.charAt(pos))) pos--;
                        int fin = pos + 1;
                        while (pos >= 0 && Character.isLetterOrDigit(texto.charAt(pos))) pos--;
                        start = pos + 1;
                        end = fin;
                    }

                    if (start >= end || ultimoColorHTML == null) return;

                    String original = areatexto.getDocument().getText(start, end - start);
                    String nuevoTexto;

                    if (ultimoColorHTML.equals("#FFFFFF")) {
                        nuevoTexto = "<span style=\"background-color:#FFFFFF; color:#000000\">" + original + "</span>";
                    } else {
                        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
                    }

                    javax.swing.text.Document doc = areatexto.getDocument();
                    doc.remove(start, end - start);
                    kit.insertHTML((HTMLDocument) doc, start, nuevoTexto, 0, 0, HTML.Tag.SPAN);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Alt + C: cambiar al siguiente color
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("alt C"), "siguienteColor");
        areatexto.getActionMap().put("siguienteColor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] colores = {"#808080", "#90EE90", "#CC9900", "#87CEEB"};
                int index = -1;
                for (int i = 0; i < colores.length; i++) {
                    if (colores[i].equalsIgnoreCase(ultimoColorHTML)) {
                        index = i;
                        break;
                    }
                }
                int siguiente = (index + 1) % colores.length;
                ultimoColorHTML = colores[siguiente];
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });

        // Alt + X: cambiar al color "Sin destacar"
        areatexto.getInputMap().put(KeyStroke.getKeyStroke("alt X"), "colorSinDestacar");
        areatexto.getActionMap().put("colorSinDestacar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ultimoColorHTML = "#FFFFFF";
                Toolkit.getDefaultToolkit().beep(); // Confirmación
            }
        });


       
        //Boton de guardado
        JButton guardar = new JButton("Guardar cambios");
        guardar.addActionListener(e -> {
        	StyledDocument styledDoc = areatexto.getStyledDocument();
            int imgIndex = 0;

            try {
                // Recorre el documento para detectar imágenes
                for (int i = 0; i < doc.getLength(); i++) {
                    Element elem = doc.getCharacterElement(i);
                    AttributeSet attr = elem.getAttributes();
                    Icon icon = StyleConstants.getIcon(attr);

                    if (icon instanceof ImageIcon) {
                        ImageIcon imgIcon = (ImageIcon) icon;
                        String nombreArchivo = limpiarTitulo(not.titulo) + "_img" + imgIndex + ".png";

                        // Guardar imagen en disco
                        File imgDir = new File("imagenes");
                        if (!imgDir.exists()) imgDir.mkdir();
                        File output = new File(imgDir, nombreArchivo);
                        ImageIO.write(toBufferedImage(imgIcon.getImage()), "png", output);

                        // Reemplazar ícono por <img src=...>
                        SimpleAttributeSet sas = new SimpleAttributeSet();
                        StyleConstants.setIcon(sas, null);  // Borra ícono visual
                        doc.remove(elem.getStartOffset(), 1);

                        kit.insertHTML((HTMLDocument) doc, elem.getStartOffset(),
                            "<img src='" + nombreArchivo + "' width='" + imgIcon.getIconWidth()
                            + "' height='" + imgIcon.getIconHeight() + "'>", 0, 0, HTML.Tag.IMG);

                        imgIndex++;
                    }
                }

                // Generar HTML final ya con <img src='...'>
                writer = new StringWriter(); // reinicia
                kit.write(writer, doc, 0, doc.getLength());
                not.contenidoHTML = writer.toString();

                // Guardar notas
                ArrayList<Nota> respaldo = Collections.list(Libreta.Notas.elements());
                Nota.guardarNotas(respaldo);

                VentanaNota.this.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


      //Spinner
        Integer[] datos = {8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100 };

        JSpinner lista = new JSpinner(new SpinnerListModel(datos));
        lista.setPreferredSize(new Dimension(100, 30));
        lista.setVisible(true);
        lista.setValue(16);

        lista.addChangeListener(e -> {
            int nuevoTamaño = (int) lista.getValue();
            
            javax.swing.text.html.StyleSheet estilo = kit.getStyleSheet();

            // Elimina reglas anteriores y aplica nuevo tamaño
            estilo.addRule("body { font-size: " + nuevoTamaño + "pt; }");

            // Recarga el contenido actual para aplicar el nuevo estilo
            String htmlActual = areatexto.getText();
            areatexto.setText("");           // limpia para forzar recarga
            areatexto.setText(htmlActual);   // recarga con nueva hoja de estilo
        });
   

        //Jlist seleccion de color 
        String[] colores = {"Gris", "Verde", "Amarillo", "Celeste","Sin destacar"};
        JList<String> listacolor = new JList<>(colores);

        listacolor.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            	try {
            	    int start = areatexto.getSelectionStart();
            	    int end = areatexto.getSelectionEnd();
            	    if (start == end) return;

            	    String seleccionado = listacolor.getSelectedValue();
            	    String original = areatexto.getSelectedText();
            	    String nuevoTexto;

            	    switch (seleccionado) {
            	    case "Gris":
            	        ultimoColorHTML = "#808080";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Verde":
            	        ultimoColorHTML = "#90EE90";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Amarillo":
            	        ultimoColorHTML = "#CC9900";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Celeste":
            	        ultimoColorHTML = "#87CEEB";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    case "Sin destacar":
            	        ultimoColorHTML = "#FFFFFF";
            	        nuevoTexto = "<span style=\"background-color:" + ultimoColorHTML + "; color:#000000\">" + original + "</span>";
            	        listacolor.clearSelection();
            	        break;
            	    default:
            	        nuevoTexto = original;
            	}
          
            	    doc.remove(start, end - start);
            	    kit.insertHTML((HTMLDocument) doc, start, nuevoTexto, 0, 0, HTML.Tag.SPAN);


            	} catch (Exception ex) {
            	    ex.printStackTrace();
            	}

            	listacolor.setVisible(false);



            }
        });
        
        
        listacolor.setVisible(false);
        
        
        //Boton para cambiar estilo
        JButton destacar = new JButton("Destacar");
        destacar.addActionListener(e -> {
        	if (listacolor.isVisible()==false) {
            	listacolor.setVisible(true);  
        	} else {
        		listacolor.setVisible(false);
        	}
        });       
        
        this.add(panelInferior, BorderLayout.SOUTH);
        panelInferior.add(destacar);
        panelInferior.add(listacolor);
        panelInferior.add(guardar);
        panelInferior.add(lista);

        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }
	
}
